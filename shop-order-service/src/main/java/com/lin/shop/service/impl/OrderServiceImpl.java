package com.lin.shop.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.lin.entity.MQEntity;
import com.lin.shop.constant.ShopCode;
import com.lin.shop.exception.CastException;
import com.lin.shop.mapper.TradeMqProducerTempMapper;
import com.lin.shop.mapper.TradeOrderMapper;
import com.lin.shop.pojo.*;
import com.lin.shop.service.ICouponService;
import com.lin.shop.service.IGoodsService;
import com.lin.shop.service.IOrderService;
import com.lin.shop.service.IUserService;
import com.lin.shop.util.IDWorker;
import com.lin.util.Result;
import javassist.expr.Cast;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 提供者实现订单服务
 */
@Slf4j
@Component
@Service(interfaceClass = IOrderService.class)
public class OrderServiceImpl implements IOrderService {


    @Reference
    private IGoodsService goodsService;

    @Reference
    private IUserService userService;

    @Reference
    private ICouponService couponService;

    @Autowired
    private IDWorker idWorker;

    @Autowired
    private TradeOrderMapper orderMapper;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private TradeMqProducerTempMapper mqProducerTempMapper;


    @Value("${mq.order.topic}")
    private String topic;

    @Value("${mq.order.tag.cancel}")
    private String tag;

    @Value("${rocketmq.producer.group}")
    private String produceGroup;

    /**
     * 确认订单
     * @param order 订单
     * @return
     */
    @Override
        public Result confirmOrder(TradeOrder order) {

        //1.校验订单

        checkOrder(order);

        //2.生成预订单 订单号不可见
        log.info("开始生成预订单");
        Long orderId = savePreOrder(order);
        log.info("生成预订单成功");

        try{

            log.info("开始扣库存");
            //3. 扣库存
            reduceGoodsNum(order);
            log.info("扣库存成功");

            log.info("开始扣优惠券");
            //4. 扣优惠券
            updateCouponStatus(order);
            log.info("扣优惠券成功");

            log.info("开始扣余额");
            //5. 扣减余额
            reduceMoneyPaid(order);
            log.info("扣余额成功");

            //模拟抛出异常
//            CastException.cast(ShopCode.SHOP_FAIL);

            //6.确认订单 订单号可见
            updateOrderStatus(order);


            //7.返回成功状态
            return new Result(ShopCode.SHOP_SUCCESS.getSuccess(),ShopCode.SHOP_SUCCESS.getMessage());

        }catch (Exception e){

            e.printStackTrace();

            log.info("订单:" + orderId + "异常！");
            //1. 3 4 5 中其中一步失败 则确认订单失败 MQ发送失败消息

            //订单ID 商品ID 优惠券ID 用户ID 余额 商品数量
            MQEntity mqEntity = new MQEntity();
            mqEntity.setOrderId(orderId);
            mqEntity.setCouponId(order.getCouponId());
            mqEntity.setGoodsId(order.getGoodsId());
            mqEntity.setGoodsNumber(order.getGoodsNumber());
            mqEntity.setUserId(order.getUserId());
            mqEntity.setUserMoney(order.getMoneyPaid());

            String keys = String.valueOf(orderId);

            //发送订单取消消息
            sendOrderCancelMQMessage(topic,tag,keys, JSON.toJSONString(mqEntity));
            log.info("订单:" + orderId + " 成功发送异常消息！");

            //2.返回失败状态
            return new Result(ShopCode.SHOP_FAIL.getSuccess(),ShopCode.SHOP_FAIL.getMessage());

        }

    }

    @Override
    public TradeOrder findOne(Long orderId) {
        if (orderId == null){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        return orderMapper.selectByPrimaryKey(orderId);
    }

    /**
     * 发送订单取消消息
     * @param topic
     * @param tag
     * @param key
     * @param body
     */
    private void sendOrderCancelMQMessage(String topic, String tag, String key, String body) {

        try {
            Message message = new Message(topic,tag,key,body.getBytes());
            SendResult  result = rocketMQTemplate.getProducer().send(message);

            TradeMqProducerTemp mqProducerTemp = new TradeMqProducerTemp();
            mqProducerTemp.setId(result.getMsgId());
            mqProducerTemp.setGroupName(produceGroup);
            mqProducerTemp.setMsgTopic(topic);
            mqProducerTemp.setMsgTag(tag);
            mqProducerTemp.setMsgKey(key);
            mqProducerTemp.setMsgBody(body);
            mqProducerTemp.setMsgStatus(0);
            mqProducerTemp.setCreateTime(new Date());
            mqProducerTempMapper.insert(mqProducerTemp);

        } catch (MQClientException e) {
            e.printStackTrace();
        } catch (RemotingException e) {
            e.printStackTrace();
        } catch (MQBrokerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    /**
     * 确认订单
     * @param order
     */
    private void updateOrderStatus(TradeOrder order) {
        order.setOrderStatus(ShopCode.SHOP_ORDER_CONFIRM.getCode());
        order.setPayStatus(ShopCode.SHOP_ORDER_PAY_STATUS_NO_PAY.getCode());
        order.setConfirmTime(new Date());
        int r = orderMapper.updateByPrimaryKey(order);
        if (r<0){
            CastException.cast(ShopCode.SHOP_ORDER_CONFIRM_FAIL);
        }
        log.info("订单:" + order.getOrderId() + "确认订单");
    }

    /**
     * 扣减余额
     * @param order
     */
    private void reduceMoneyPaid(TradeOrder order) {
        if (order != null && order.getMoneyPaid().compareTo(BigDecimal.ZERO) == 1){
            TradeUserMoneyLog tradeUserMoneyLog = new TradeUserMoneyLog();

            tradeUserMoneyLog.setUserId(order.getUserId());
            tradeUserMoneyLog.setOrderId(order.getOrderId());
            tradeUserMoneyLog.setUseMoney(order.getMoneyPaid());
            tradeUserMoneyLog.setMoneyLogType(ShopCode.SHOP_USER_MONEY_PAID.getCode());

            //扣减余额
            Result result = userService.updateMoneyPaid(tradeUserMoneyLog);

            if (result.getSuccess().equals(ShopCode.SHOP_FAIL)){
                CastException.cast(ShopCode.SHOP_USER_MONEY_REDUCE_FAIL);
            }
            log.info("订单:" + order.getOrderId() + "扣减余额成功");


        }
    }

    /**
     * 使用优惠券
     * @param order
     */
    private void updateCouponStatus(TradeOrder order) {
        if (order.getCouponId() != null){
            TradeCoupon tradeCoupon = couponService.findOne(order.getCouponId());
            tradeCoupon.setOrderId(order.getOrderId());
            tradeCoupon.setIsUsed(ShopCode.SHOP_COUPON_ISUSED.getCode());
            tradeCoupon.setUsedTime(new Date());

            //更新优惠券状态
            Result result = couponService.updateCouponStatus(tradeCoupon);

            if (result.getSuccess().equals(ShopCode.SHOP_FAIL)){
                CastException.cast(ShopCode.SHOP_COUPON_USE_FAIL);
            }
            log.info("订单:" + order.getOrderId() + "使用优惠券成功");
        }
    }

    /**
     * 扣减商品库存
     * @param order
     */
    private void reduceGoodsNum(TradeOrder order) {
        //goodsId orderId goodsNumber
        TradeGoodsNumberLog tradeGoodsNumberLog = new TradeGoodsNumberLog();

        tradeGoodsNumberLog.setOrderId(order.getOrderId());
        tradeGoodsNumberLog.setGoodsId(order.getGoodsId());
        tradeGoodsNumberLog.setGoodsNumber(order.getGoodsNumber());

        log.info("订单:" + order.getOrderId() + "准备扣余额");
        Result result = goodsService.reduceGoodsNum(tradeGoodsNumberLog);
        log.info("订单:" + order.getOrderId() + "扣余额成功");

        //扣减商品库存失败
        if (result.getSuccess().equals(ShopCode.SHOP_FAIL)){
            CastException.cast(ShopCode.SHOP_REDUCE_GOODS_NUM_FAIL);
        }
        log.info("订单:" + order.getOrderId() + "扣减库存成功");

    }

    /**
     * 生成预订单
     * @param order
     */
    private Long savePreOrder(TradeOrder order) {

        //1. 设置预订单不可见
        order.setOrderStatus(ShopCode.SHOP_ORDER_NO_CONFIRM.getCode());

        //2. 设置订单ID
        Long orderId = idWorker.nextId();
        order.setOrderId(orderId);

        //3. 核算订单运费
        BigDecimal shippingFee = calculateShippingFee(order.getOrderAmount());
        if (order.getShippingFee().compareTo(shippingFee) != 0){
            CastException.cast(ShopCode.SHOP_ORDER_SHIPPINGFEE_INVALID);
        }

        //4. 核算订单订单总金额是否合法
        BigDecimal orderAmount = order.getGoodsPrice().multiply(new BigDecimal(order.getGoodsNumber()));
        //添加运费
        orderAmount.add(shippingFee);
        if (order.getOrderAmount().compareTo(orderAmount) != 0){
            CastException.cast(ShopCode.SHOP_ORDERAMOUNT_INVALID);
        }

        //5. 判断用户是否使用余额
        BigDecimal moneyPaid = order.getMoneyPaid();
        if (moneyPaid != null){
            int r = moneyPaid.compareTo(BigDecimal.ZERO);

            //余额小于0
            if (r == -1){
                CastException.cast(ShopCode.SHOP_MONEY_PAID_LESS_ZERO);
            }

            //余额大于0
            if (r == 1){
               TradeUser tradeUser =  userService.findOne(order.getUserId());
               //订单金额大于用户余额
               if (moneyPaid.compareTo(new BigDecimal(tradeUser.getUserMoney())) == 1){
                   CastException.cast(ShopCode.SHOP_MONEY_PAID_INVALID);
               }
            }
        }else{
            //未使用余额
            order.setMoneyPaid(BigDecimal.ZERO);
        }

        //6. 判断用户是否使用优惠券
        Long couponId = order.getCouponId();
        if (couponId != null) {
            TradeCoupon tradeCoupon = couponService.findOne(couponId);
            //优惠券不存在
            if (tradeCoupon == null){
                CastException.cast(ShopCode.SHOP_COUPON_NO_EXIST);
            }
            //优惠券存在
            if (tradeCoupon != null){
                //优惠券已经使用
                if (tradeCoupon.getIsUsed().intValue() == ShopCode.SHOP_COUPON_ISUSED.getCode()){
                    CastException.cast(ShopCode.SHOP_COUPON_ISUSED);
                }
            }
            order.setCouponPaid(tradeCoupon.getCouponPrice());
        }else{
            //未使用优惠券
            order.setCouponPaid(BigDecimal.ZERO);
        }

        //7. 核算订单支付金额
        //订单金额 = 总金额 - 余额 - 优惠券金额
        BigDecimal payAmount = order.getOrderAmount() .subtract(order.getMoneyPaid()) .subtract(order.getCouponPaid());
        order.setPayAmount(payAmount);

        //8. 设置下单时间
        order.setAddTime(new Date());

        //9. 存放数据库
        orderMapper.insert(order);

        //10 返回订单ID

        return orderId;



    }

    /**
     * 核算订单运费
     * @param orderAmount
     * @return BigDecimal
     */
    private BigDecimal calculateShippingFee(BigDecimal orderAmount) {

        //订单金额大于100 不用运费 否则10元运费
        if (orderAmount.compareTo( new BigDecimal(100)) == 1){
            return BigDecimal.ZERO;
        }
        return new BigDecimal(10);
    }

    /**
     * 校验订单
     * @param order
     */
    private void checkOrder(TradeOrder order) {

        //1.校验订单是否存在
        if (order==null){
            CastException.cast(ShopCode.SHOP_ORDER_INVALID);
        }

        log.info("goodsService:[{}]",goodsService );
        log.info("order:[{}]",order);

        //2.校验订单商品是否存在
        Long goodsId = order.getGoodsId();
        TradeGoods tradeGoods  =  goodsService.findOne(goodsId);
        if (tradeGoods == null){
            CastException.cast(ShopCode.SHOP_GOODS_NO_EXIST);
        }

        //3.校验下单用户是否合法
        Long userId = order.getUserId();
        TradeUser tradeUser =  userService.findOne(userId);
        if (tradeUser == null){
            CastException.cast(ShopCode.SHOP_USER_NO_EXIST);
        }

        //4.校验下单金额是否合法
        // 下单价格  != 商品价格
        if ( order.getGoodsPrice().compareTo(tradeGoods.getGoodsPrice()) != 0 ){
            CastException.cast(ShopCode.SHOP_GOODS_PRICE_INVALID);
        }

        //5.校验订单商品是否合法
        //下单商品数量 >  库存
        if(order.getGoodsNumber() > tradeGoods.getGoodsNumber()){
            CastException.cast(ShopCode.SHOP_GOODS_NUM_NOT_ENOUGH);
        }

        log.info("校验订单通过！");
    }

}
