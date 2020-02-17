package com.lin.shop.mq;

import com.alibaba.fastjson.JSON;
import com.lin.entity.MQEntity;
import com.lin.shop.constant.ShopCode;
import com.lin.shop.mapper.TradeCouponMapper;
import com.lin.shop.pojo.TradeCoupon;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *  订单异常 优惠券返回
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.order.topic}",
        consumerGroup = "${mq.order.consumer.group.name}", messageModel = MessageModel.BROADCASTING)
public class OrderCancelMQListener implements RocketMQListener<MessageExt> {

    @Autowired
    private TradeCouponMapper couponMapper;

    @Override
    public void onMessage(MessageExt messageExt) {


        log.info("优惠券回退服务监听到消息");


        try {
            //1. 解析内容

            String body = new String(messageExt.getBody(), "UTF-8");
            MQEntity mqEntity = JSON.parseObject(body,MQEntity.class);


            Long couponId = mqEntity.getCouponId();
            if (couponId != null) {
                //2. 查询优惠券
                TradeCoupon coupon = couponMapper.selectByPrimaryKey(couponId);

                //3. 更改优惠券状态
                coupon.setIsUsed(ShopCode.SHOP_COUPON_UNUSED.getCode());
                coupon.setOrderId(null);
                coupon.setUsedTime(null);
                couponMapper.updateByPrimaryKey(coupon);


                log.info("订单:" + mqEntity.getOrderId() + "优惠券回退成功");
            }


        }catch (Exception e) {

            e.printStackTrace();
            log.info("优惠券回退失败");
        }


    }
}
