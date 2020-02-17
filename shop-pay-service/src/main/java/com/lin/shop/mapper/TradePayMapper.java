package com.lin.shop.mapper;

import com.lin.shop.pojo.TradePay;
import com.lin.shop.pojo.TradePayExample;
import com.lin.shop.pojo.TradePayKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TradePayMapper {
    int countByExample(TradePayExample example);

    int deleteByExample(TradePayExample example);

    int deleteByPrimaryKey(TradePayKey key);

    int insert(TradePay record);

    int insertSelective(TradePay record);

    List<TradePay> selectByExample(TradePayExample example);

    TradePay selectByPrimaryKey(TradePayKey key);

    int updateByExampleSelective(@Param("record") TradePay record, @Param("example") TradePayExample example);

    int updateByExample(@Param("record") TradePay record, @Param("example") TradePayExample example);

    int updateByPrimaryKeySelective(TradePay record);

    int updateByPrimaryKey(TradePay record);
}