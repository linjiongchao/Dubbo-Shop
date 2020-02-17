package com.lin.shop.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.lin.shop.constant.ShopCode;
import com.lin.shop.exception.CastException;
import com.lin.shop.mapper.TradeGoodsMapper;
import com.lin.shop.mapper.TradeGoodsNumberLogMapper;
import com.lin.shop.pojo.TradeGoods;
import com.lin.shop.pojo.TradeGoodsNumberLog;
import com.lin.shop.service.IGoodsService;
import com.lin.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
@Service(interfaceClass = IGoodsService.class)
public class GoodsServiceImpl implements IGoodsService {

    @Autowired
    private TradeGoodsMapper goodsMapper;

    @Autowired
    private TradeGoodsNumberLogMapper goodsNumberLogMapper;

    @Override
    public TradeGoods findOne(Long goodsId) {
        if (goodsId == null) {
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        return goodsMapper.selectByPrimaryKey(goodsId);
    }


    @Override
    public Result reduceGoodsNum(TradeGoodsNumberLog goodsNumberLog) {
        if (goodsNumberLog == null ||
                goodsNumberLog.getOrderId() == null ||
                goodsNumberLog.getGoodsNumber() == null ||
                goodsNumberLog.getGoodsNumber() == null){

            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
            return new Result(ShopCode.SHOP_FAIL.getSuccess(),ShopCode.SHOP_FAIL.getMessage());
        }


        log.info("根据:" + goodsNumberLog.getGoodsId() + "查商品");
        TradeGoods tradeGoods = goodsMapper.selectByPrimaryKey(goodsNumberLog.getGoodsId());
        log.info("根据:" + goodsNumberLog.getGoodsId() + "查商品成功");

        //商品库存不足
        if (tradeGoods.getGoodsNumber() < goodsNumberLog.getGoodsNumber()){
            CastException.cast(ShopCode.SHOP_GOODS_NUM_NOT_ENOUGH);
            return new Result(ShopCode.SHOP_FAIL.getSuccess(),ShopCode.SHOP_FAIL.getMessage());
        }

        log.info("更新商品库存");
        //更新库存
        tradeGoods.setGoodsNumber(tradeGoods.getGoodsNumber() - goodsNumberLog.getGoodsNumber());
        goodsMapper.updateByPrimaryKey(tradeGoods);
        log.info("更新商品库存成功");

        //记录日志
        //加库存正数 减库存负数
        log.info("记录商品库存日志");

        goodsNumberLog.setGoodsNumber(-(goodsNumberLog.getGoodsNumber()));
        goodsNumberLog.setLogTime(new Date());
        goodsNumberLogMapper.insert(goodsNumberLog);

        log.info("记录商品库存日志成功");

        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(),ShopCode.SHOP_SUCCESS.getMessage());
    }
}
