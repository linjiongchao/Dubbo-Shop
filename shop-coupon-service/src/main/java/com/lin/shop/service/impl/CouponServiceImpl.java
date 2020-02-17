package com.lin.shop.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.lin.shop.constant.ShopCode;
import com.lin.shop.exception.CastException;
import com.lin.shop.mapper.TradeCouponMapper;
import com.lin.shop.pojo.TradeCoupon;
import com.lin.shop.service.ICouponService;
import com.lin.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Service(interfaceClass = ICouponService.class)
public class CouponServiceImpl implements ICouponService{

    @Autowired
    private TradeCouponMapper tradeCouponMapper;

    @Override
    public TradeCoupon findOne(Long couponId) {
        if (couponId == null){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        return tradeCouponMapper.selectByPrimaryKey(couponId);
    }

    @Override
    public Result updateCouponStatus(TradeCoupon tradeCoupon) {
        if (tradeCoupon  == null || tradeCoupon.getCouponId() == null){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
            return new Result(ShopCode.SHOP_FAIL.getSuccess(),ShopCode.SHOP_FAIL.getMessage());
        }
        tradeCouponMapper.updateByPrimaryKey(tradeCoupon);
        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(),ShopCode.SHOP_SUCCESS.getMessage());
    }
}
