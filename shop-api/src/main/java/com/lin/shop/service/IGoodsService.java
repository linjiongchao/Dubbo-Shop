package com.lin.shop.service;

import com.lin.shop.pojo.TradeGoods;
import com.lin.shop.pojo.TradeGoodsNumberLog;
import com.lin.util.Result;

/**
 * 商品服务
 */
public interface IGoodsService {

    /**
     * 根据商品ID查对象
     * @param goodsId
     * @return
     */
    TradeGoods findOne(Long goodsId);

    /**
     * 扣减库存
     * @param tradeGoodsNumberLog
     * @return
     */
    Result reduceGoodsNum(TradeGoodsNumberLog tradeGoodsNumberLog);
}
