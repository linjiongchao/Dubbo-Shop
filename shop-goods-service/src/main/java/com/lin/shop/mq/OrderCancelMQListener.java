package com.lin.shop.mq;

import com.alibaba.fastjson.JSON;
import com.lin.entity.MQEntity;
import com.lin.shop.constant.ShopCode;
import com.lin.shop.mapper.TradeGoodsMapper;
import com.lin.shop.mapper.TradeGoodsNumberLogMapper;
import com.lin.shop.mapper.TradeMqConsumerLogMapper;
import com.lin.shop.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.Date;

/**
 * 订单异常 商品回退库存
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.order.topic}",
        consumerGroup = "${mq.order.consumer.group.name}", messageModel = MessageModel.BROADCASTING)
public class OrderCancelMQListener implements RocketMQListener<MessageExt> {

    @Autowired
    private TradeMqConsumerLogMapper mqConsumerLogMapper;

    @Autowired
    private TradeGoodsMapper goodsMapper;

    @Autowired
    private TradeGoodsNumberLogMapper goodsNumberLogMapper;

    @Value("${mq.order.consumer.group.name}")
    private String consumerGroup;



    @Override
    public void onMessage(MessageExt messageExt) {


        String tags = null;
        String keys = null;
        String body = null;
        TradeMqConsumerLogKey mqConsumerLogKey = null;

        log.info("商品回退服务监听到消息");

        try {
            //1. 解析内容
            tags = messageExt.getTags();
            keys = messageExt.getKeys();
            body = new String(messageExt.getBody(),"UTF-8");

            //2. 查询消息消费记录
            mqConsumerLogKey = new TradeMqConsumerLogKey();
            mqConsumerLogKey.setMsgTag(tags);
            mqConsumerLogKey.setMsgKey(keys);
            mqConsumerLogKey.setGroupName(consumerGroup);
            TradeMqConsumerLog mqConsumerLog = mqConsumerLogMapper.selectByPrimaryKey(mqConsumerLogKey);
            if (mqConsumerLog!=null){
                //3. 消息已经消费过

                //获取消费状态
                Integer consumerStatus =  mqConsumerLog.getConsumerStatus();

                //3.1 处理成功  返回
                if(ShopCode.SHOP_MQ_MESSAGE_STATUS_SUCCESS.getCode().intValue() == consumerStatus.intValue()){
                    log.info("消息已处理成功");
                    return;
                }

                //3.2 正在处理 返回
                if(ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode().intValue() == consumerStatus.intValue()){
                    log.info("消息正在处理");
                    return;
                }

                //3.3 处理失败
                if(ShopCode.SHOP_MQ_MESSAGE_STATUS_FAIL.getCode().intValue() == consumerStatus.intValue()){

                    // 获取消费次数
                    Integer times = mqConsumerLog.getConsumerTimes();

                    //  消费次数 > 3 拒绝处理
                    if(times.intValue()>3){
                        log.info("消息处理超过3次 不能再处理了");
                        return;
                    }

                    // 设置消息正在处理
                    mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode());


                    //数据库乐观锁 修改消息正在处理
                    TradeMqConsumerLogExample mqConsumerLogExample = new TradeMqConsumerLogExample();
                    TradeMqConsumerLogExample.Criteria criteria = mqConsumerLogExample.createCriteria();
                    criteria.andGroupNameEqualTo(consumerGroup);
                    criteria.andMsgTagEqualTo(tags);
                    criteria.andMsgKeyEqualTo(keys);
                    criteria.andConsumerTimesEqualTo(mqConsumerLog.getConsumerTimes());

                    int r = mqConsumerLogMapper.updateByExampleSelective(mqConsumerLog,mqConsumerLogExample);

                    if (r<=0){
                        //未修改成功
                        log.info("并发修改 稍后处理");
                        return;
                    }

                }

            }else{
                //4. 消息未消费过
                mqConsumerLog = new TradeMqConsumerLog();
                mqConsumerLog.setMsgTag(tags);
                mqConsumerLog.setMsgKey(keys);
                mqConsumerLog.setGroupName(consumerGroup);
                mqConsumerLog.setMsgId(messageExt.getMsgId());
                mqConsumerLog.setMsgBody(body);
                //正在处理
                mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode());
                mqConsumerLog.setConsumerTimestamp(new Date());
                //消费次数 0
                mqConsumerLog.setConsumerTimes(0);

                //添加 正在处理的消费日志
                mqConsumerLogMapper.insert(mqConsumerLog);

            }


            //5. 回退库存
            MQEntity mqEntity = JSON.parseObject(body,MQEntity.class);
            Long goodsId = mqEntity.getGoodsId();
            TradeGoods goods = goodsMapper.selectByPrimaryKey(goodsId);
            goods.setGoodsNumber(goods.getGoodsNumber() + mqEntity.getGoodsNumber());
            goodsMapper.updateByPrimaryKey(goods);

            //6. 记录库存日志

            TradeGoodsNumberLog goodsNumberLog = new TradeGoodsNumberLog();
            goodsNumberLog.setGoodsId(goodsId);
            goodsNumberLog.setOrderId(mqEntity.getOrderId());
            goodsNumberLog.setGoodsNumber(mqEntity.getGoodsNumber());
            goodsNumberLog.setLogTime(new Date());

            log.info("goodsNumberLog:" + JSON.toJSONString(goodsNumberLog));
            goodsNumberLogMapper.insert(goodsNumberLog);



            //7. 消息状态修改为成功
            mqConsumerLog.setConsumerStatus(ShopCode.SHOP_SUCCESS.getCode());
            mqConsumerLog.setConsumerTimestamp(new Date());
            mqConsumerLogMapper.updateByPrimaryKey(mqConsumerLog);


            log.info("订单:" +  mqEntity.getOrderId() + "商品库存回退成功");

        } catch (Exception e) {

            e.printStackTrace();

            //8. 消费失败

            mqConsumerLogKey = new TradeMqConsumerLogKey();
            mqConsumerLogKey.setMsgTag(tags);
            mqConsumerLogKey.setMsgKey(keys);
            mqConsumerLogKey.setGroupName(consumerGroup);
            TradeMqConsumerLog mqConsumerLog = mqConsumerLogMapper.selectByPrimaryKey(mqConsumerLogKey);

            //如果第一次消费
            if (mqConsumerLog == null) {
                //数据库没记录

                mqConsumerLog = new TradeMqConsumerLog();
                mqConsumerLog.setMsgTag(tags);
                mqConsumerLog.setMsgKey(keys);
                mqConsumerLog.setGroupName(consumerGroup);
                mqConsumerLog.setMsgId(messageExt.getMsgId());
                mqConsumerLog.setMsgBody(body);
                //处理失败
                mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_FAIL.getCode());
                mqConsumerLog.setConsumerTimestamp(new Date());
                //消费次数 1
                mqConsumerLog.setConsumerTimes(1);
            }else{
                //消费次数  + 1
                mqConsumerLog.setConsumerTimes(mqConsumerLog.getConsumerTimes() + 1);
            }
                //添加 正在处理的消费日志
                mqConsumerLogMapper.insert(mqConsumerLog);

        }

    }


}
