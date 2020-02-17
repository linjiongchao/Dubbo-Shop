package com.lin.shop.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.lin.shop.constant.ShopCode;
import com.lin.shop.exception.CastException;
import com.lin.shop.mapper.TradeUserMapper;
import com.lin.shop.pojo.TradeOrder;
import com.lin.shop.pojo.TradePay;
import com.lin.shop.pojo.TradeUser;
import com.lin.shop.service.IOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

/**
 * 支付成功 用户增加积分
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.pay.topic}",consumerGroup = "${mq.pay.consumer.group.name}",messageModel = MessageModel.BROADCASTING)
public class PaymentMQListener implements RocketMQListener<MessageExt> {

    @Autowired
    private TradeUserMapper userMapper;

    @Reference
    private IOrderService orderService;

    @Override
    public void onMessage(MessageExt messageExt) {
        try {

            log.info("用户准备增加积分");

            //1. 解析消息内容
            String body = new String(messageExt.getBody(),"UTF-8");

            TradePay tradePay = JSON.parseObject(body, TradePay.class);

            //2. 判断消息内容
            if (tradePay == null
                    || tradePay.getPayId() == null
                    || tradePay.getOrderId() ==null
                    || tradePay.getIsPaid() == null
                    || tradePay.getPayAmount() == null){
                CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
            }

            //3. 获取用户
            log.info("orderService:[{}]",orderService);
            log.info("tradePay.getOrderId():[{}]",tradePay.getOrderId());
            TradeOrder order = orderService.findOne(tradePay.getOrderId());
            if (order == null){
                CastException.cast(ShopCode.SHOP_ORDER_INVALID);
            }
            TradeUser user = userMapper.selectByPrimaryKey(order.getUserId());

            if (user == null){
                CastException.cast(ShopCode.SHOP_USER_NO_EXIST);
            }

            //4. 修改积分
            if (user.getUserScore() == null){
                user.setUserScore(0);
            }
            user.setUserScore((user.getUserScore() + (order.getGoodsPrice().intValue()/100)));

            //5. 更新状态
            userMapper.updateByPrimaryKey(user);


            log.info("用户积分增加成功");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
