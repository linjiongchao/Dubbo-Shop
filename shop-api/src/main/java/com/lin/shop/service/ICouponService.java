package com.lin.shop.service;

import com.lin.shop.pojo.TradeCoupon;
import com.lin.util.Result;

/**
 * 优惠券服务
 */
public interface ICouponService {
    /**
     * 根据主键查对象
     * @param couponId
     * @return Result
     */
    TradeCoupon findOne(Long couponId);

    /**
     * 更新优惠券状态
     * @param tradeCoupon
     * @return Result
     */
    Result updateCouponStatus(TradeCoupon tradeCoupon);
}
