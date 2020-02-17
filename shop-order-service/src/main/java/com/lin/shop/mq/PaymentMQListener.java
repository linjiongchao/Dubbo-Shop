package com.lin.shop.mq;

import com.alibaba.fastjson.JSON;
import com.lin.entity.MQEntity;
import com.lin.shop.constant.ShopCode;
import com.lin.shop.exception.CastException;
import com.lin.shop.mapper.TradeOrderMapper;
import com.lin.shop.pojo.TradeOrder;
import com.lin.shop.pojo.TradePay;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

/**
 * 支付成功 修改订单状态服务
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.pay.topic}",consumerGroup = "${mq.pay.consumer.group.name}",messageModel = MessageModel.BROADCASTING)
public class PaymentMQListener implements RocketMQListener<MessageExt> {

    @Autowired
    private TradeOrderMapper orderMapper;


    @Override
    public void onMessage(MessageExt messageExt) {

        try {
            log.info("订单准备修改为已支付状态");


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

            //3. 获取订单
            TradeOrder order = orderMapper.selectByPrimaryKey(tradePay.getOrderId());

            if (order==null){
                CastException.cast(ShopCode.SHOP_ORDER_INVALID);
            }
            //4. 修改支付状态
            order.setPayStatus(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY.getCode());

            //5. 提交订单
            orderMapper.updateByPrimaryKeySelective(order);

            log.info("订单成功修改为已支付状态");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
