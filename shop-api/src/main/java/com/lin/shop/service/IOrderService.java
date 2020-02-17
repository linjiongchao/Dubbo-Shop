package com.lin.shop.service;

import com.lin.shop.pojo.TradeOrder;
import com.lin.util.Result;

/**
 * 订单服务
 */
public interface IOrderService {

    /**
     * 确认订单
     * @param order
     * @return Result
     */
    Result confirmOrder(TradeOrder order);

    /**
     * 通过主键查对象
     * @param orderId
     * @return
     */
    TradeOrder findOne(Long orderId);


}
