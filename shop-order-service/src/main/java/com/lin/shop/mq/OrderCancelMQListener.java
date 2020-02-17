package com.lin.shop.mq;

import com.alibaba.fastjson.JSON;
import com.lin.entity.MQEntity;
import com.lin.shop.constant.ShopCode;
import com.lin.shop.mapper.TradeOrderMapper;
import com.lin.shop.pojo.TradeMqConsumerLogKey;
import com.lin.shop.pojo.TradeOrder;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

/**
 * 订单取消服务
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.order.topic}",
        consumerGroup = "${mq.order.consumer.group.name}", messageModel = MessageModel.BROADCASTING)
public class OrderCancelMQListener implements RocketMQListener<MessageExt> {


    @Autowired
    private TradeOrderMapper orderMapper;

    @Override
    public void onMessage(MessageExt messageExt) {

        log.info("订单服务监听到内容");

        try {

        //1. 解析内容
        String body = new String(messageExt.getBody(),"UTF-8");
        MQEntity mqEntity = JSON.parseObject(body,MQEntity.class);

        if (mqEntity.getOrderId() != null) {

            //2. 获取订单
            TradeOrder order = orderMapper.selectByPrimaryKey(mqEntity.getOrderId());

            //3. 修改状态
            order.setOrderStatus(ShopCode.SHOP_ORDER_CANCEL.getCode());

            orderMapper.updateByPrimaryKey(order);

            log.info("订单取消成功");
        }else{
            log.info("订单不存在");
        }

        }catch (Exception e){
            e.printStackTrace();
            log.info("订单取消失败");
        }

    }
}
