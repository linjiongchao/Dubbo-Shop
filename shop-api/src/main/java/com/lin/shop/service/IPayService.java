package com.lin.shop.service;

import com.lin.shop.pojo.TradePay;
import com.lin.util.Result;

/**
 * 支付服务
 */
public interface IPayService {

    /**
     * 生成支付订单
     * @param tradePay
     */
    Result createPayment(TradePay tradePay);


    /**
     * 支付回调接口
     * @param tradePay
     * @return
     */
    Result callbackPayment(TradePay tradePay);
}
