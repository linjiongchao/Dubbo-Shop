package com.lin.shop.service;

import com.lin.shop.pojo.TradeUser;
import com.lin.shop.pojo.TradeUserMoneyLog;
import com.lin.util.Result;

/**
 * 用户服务
 */
public interface IUserService {

    /**
     * 根据主键查对象
     * @param userId
     */
    TradeUser findOne(Long userId);

    /**
     * 更新余额
     * @param tradeUserMoneyLog
     * @return
     */
    Result updateMoneyPaid(TradeUserMoneyLog tradeUserMoneyLog);
}
