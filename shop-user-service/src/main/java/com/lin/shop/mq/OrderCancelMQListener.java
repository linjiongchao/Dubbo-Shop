package com.lin.shop.mq;

import com.alibaba.fastjson.JSON;
import com.lin.entity.MQEntity;
import com.lin.shop.constant.ShopCode;
import com.lin.shop.pojo.TradeUserMoneyLog;
import com.lin.shop.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 用户余额回退服务
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.order.topic}",
        consumerGroup = "${mq.order.consumer.group.name}", messageModel = MessageModel.BROADCASTING)
public class OrderCancelMQListener implements RocketMQListener<MessageExt> {

    @Autowired
    private IUserService userService;

    @Override
    public void onMessage(MessageExt messageExt) {


        try {

            log.info("用户余额回退服务监听到消息");


            //1. 解析消息
            String body = new String(messageExt.getBody(),"UTF-8");
            MQEntity mqEntity = JSON.parseObject(body,MQEntity.class);

            if (mqEntity.getUserMoney() !=null ||
            mqEntity.getUserMoney().compareTo(BigDecimal.ZERO) == 1) {
                //2.调用userService回退余额
                TradeUserMoneyLog userMoneyLog = new TradeUserMoneyLog();
                userMoneyLog.setUserId(mqEntity.getUserId());
                userMoneyLog.setOrderId(mqEntity.getOrderId());
                userMoneyLog.setMoneyLogType(ShopCode.SHOP_USER_MONEY_REFUND.getCode());
                userMoneyLog.setUseMoney(mqEntity.getUserMoney());
                userService.updateMoneyPaid(userMoneyLog);

                log.info("余额回退成功");
            }else{
                log.info("余额回退失败");
            }

        }catch (Exception e){
            e.printStackTrace();
            log.info("余额回退失败");
        }
    }
}
