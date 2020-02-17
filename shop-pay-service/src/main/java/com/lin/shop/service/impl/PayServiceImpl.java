package com.lin.shop.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lin.shop.constant.ShopCode;
import com.lin.shop.exception.CastException;
import com.lin.shop.mapper.TradeMqProducerTempMapper;
import com.lin.shop.mapper.TradePayMapper;
import com.lin.shop.pojo.*;
import com.lin.shop.service.IOrderService;
import com.lin.shop.service.IPayService;
import com.lin.shop.util.IDWorker;
import com.lin.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Service(interfaceClass = IPayService.class)
public class PayServiceImpl implements IPayService {

    @Reference
    private IOrderService orderService;

    @Autowired
    private TradePayMapper payMapper;

    @Autowired
    private TradeMqProducerTempMapper mqProducerTempMapper;

    @Autowired
    private IDWorker idWorker;

    //自定义线程池
    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Value("${rocketmq.producer.group}")
    private String groupName;

    @Value("${mq.pay.topic}")
    private String topic;

    @Value("${mq.pay.tag}")
    private String tag;


    /**
     * 创建支付订单
     * @param tradePay
     * @return
     */
    @Override
    public Result createPayment(TradePay tradePay) {

        if (tradePay== null || tradePay.getOrderId() == null){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }

        //1. 判断订单支付状态
        Long orderId = tradePay.getOrderId();

        TradePayExample payExample = new TradePayExample();
        TradePayExample.Criteria criteria = payExample.createCriteria();
        criteria.andOrderIdEqualTo(orderId);
        criteria.andIsPaidEqualTo(ShopCode.SHOP_PAYMENT_IS_PAID.getCode());

            int r = payMapper.countByExample(payExample);

        //已支付
        if (r>0){
            CastException.cast(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY);
            return new Result(ShopCode.SHOP_FAIL.getSuccess(),ShopCode.SHOP_FAIL.getMessage());
        }

        //2. 设置订单未支付
        tradePay.setIsPaid(ShopCode.SHOP_ORDER_PAY_STATUS_NO_PAY.getCode());
        tradePay.setPayId(idWorker.nextId());

        //3. 保存支付订单
        payMapper.insert(tradePay);

        //4. 返回结果
        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(),ShopCode.SHOP_SUCCESS.getMessage());
    }

    /**
     * 支付回调接口
     * @param tradePay
     * @return
     */
    @Override
    public Result callbackPayment(TradePay tradePay) {

        //1. 判断用户支付状态
        if (tradePay.getPayId() != null ||
            tradePay.getOrderId()!=null) {

            TradePayKey payKey = new TradePay();
            payKey.setPayId(tradePay.getPayId());
            payKey.setOrderId(tradePay.getOrderId());

            TradePay pay = payMapper.selectByPrimaryKey(payKey);
            if (pay!=null
                    || pay.getIsPaid()!=null
                    || pay.getIsPaid().intValue() == ShopCode.SHOP_ORDER_PAY_STATUS_NO_PAY.getCode().intValue()) {

                //2. 更新支付订单状态
                pay.setIsPaid(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY.getCode());
                int r = payMapper.updateByPrimaryKeySelective(pay);

                if (r == 1){
                    log.info("支付状态更改为已支付");
                    //3. 创建支付成功消息
                    TradeMqProducerTemp mqProducerTemp = new TradeMqProducerTemp();
                    mqProducerTemp.setId(String.valueOf(idWorker.nextId()));
                    mqProducerTemp.setGroupName(groupName);
                    mqProducerTemp.setMsgTopic(topic);
                    mqProducerTemp.setMsgTag(tag);
                    mqProducerTemp.setMsgKey(String.valueOf(pay.getPayId()));
                    mqProducerTemp.setMsgBody(JSONObject.toJSONString(pay));
                    mqProducerTemp.setMsgStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_SUCCESS.getCode());
                    mqProducerTemp.setCreateTime(new Date());

                    //4. 将支付成功消息持久化数据库
                    mqProducerTempMapper.insert(mqProducerTemp);
                    log.info("持久化到数据库的消息添加成功");

                    /**
                     * 使用自定义线程池发送
                     */
                    Future future =  threadPoolTaskExecutor.submit(()->{
                        log.info("准备发送消息");
                        //5. 发送MQ消息
                        SendResult result = sendMQMessage(topic,tag,String.valueOf(pay.getPayId()),JSONObject.toJSONString(pay));
                        //6. 等待支付结果 删除发送成功消息
                        if (result !=null
                                ||result.getSendStatus() == SendStatus.SEND_OK){
                            log.info("发送消息成功");
                            mqProducerTempMapper.deleteByPrimaryKey(mqProducerTemp.getId());
                            log.info("持久化到数据库的消息删除成功");
                        }
                    });



                    return new Result(ShopCode.SHOP_SUCCESS.getSuccess(),ShopCode.SHOP_SUCCESS.getMessage());
                }else{
                    log.info("支付状态更改失败");
                    return  new Result(ShopCode.SHOP_FAIL.getSuccess(), ShopCode.SHOP_FAIL.getMessage());
                }
        }
        }else {
            CastException.cast(ShopCode.SHOP_PAYMENT_NOT_FOUND);
            return  new Result(ShopCode.SHOP_FAIL.getSuccess(), ShopCode.SHOP_FAIL.getMessage());
        }
        return  new Result(ShopCode.SHOP_FAIL.getSuccess(), ShopCode.SHOP_FAIL.getMessage());
    }

    /**
     * 发送MQ消息
     * @param topic
     * @param tag
     * @param keys
     * @param body
     * @return
     */
    private SendResult sendMQMessage(String topic,String tag,String keys,String body) {

        try {

            if (StringUtils.isEmpty(tag)
                    || StringUtils.isEmpty(tag)
                    || StringUtils.isEmpty(keys)
                    || StringUtils.isEmpty(body)) {
                CastException.cast(ShopCode.SHOP_MQ_MESSAGE_STATUS_FAIL);
            }

            Message message = new Message(topic, tag, keys, body.getBytes());
            SendResult result = rocketMQTemplate.getProducer().send(message);
            TimeUnit.SECONDS.sleep(2);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            CastException.cast(ShopCode.SHOP_MQ_MESSAGE_STATUS_FAIL);
            return null;
        }
    }
}
