package com.lin.shop.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.lin.shop.constant.ShopCode;
import com.lin.shop.exception.CastException;
import com.lin.shop.mapper.TradeUserMapper;
import com.lin.shop.mapper.TradeUserMoneyLogMapper;
import com.lin.shop.pojo.TradeUser;
import com.lin.shop.pojo.TradeUserMoneyLog;
import com.lin.shop.pojo.TradeUserMoneyLogExample;
import com.lin.shop.service.IUserService;
import com.lin.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

@Component
@Service(interfaceClass = IUserService.class)
public class UserServiceImpl implements IUserService{

    @Autowired
    private TradeUserMapper userMapper;

    @Autowired
    private TradeUserMoneyLogMapper userMoneyLogMapper;


    @Override
    public TradeUser findOne(Long userId) {
        if (userId == null)
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        return userMapper.selectByPrimaryKey(userId);
    }

    @Override
    public Result updateMoneyPaid(TradeUserMoneyLog userMoneyLog) {
        // 1. 校验参数是否合法
        if (userMoneyLog == null ||
                userMoneyLog.getOrderId() == null ||
                userMoneyLog.getUserId() == null ||
                userMoneyLog.getUseMoney() == null ||
                userMoneyLog.getUseMoney().compareTo(BigDecimal.ZERO) <= 0){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }


        //2. 查询余额使用日志
        TradeUserMoneyLogExample userMoneyLogExample  = new TradeUserMoneyLogExample();
        TradeUserMoneyLogExample.Criteria criteria = userMoneyLogExample.createCriteria();

        criteria.andOrderIdEqualTo(userMoneyLog.getOrderId());
        criteria.andUserIdEqualTo(userMoneyLog.getUserId());

        // 通过orderId userID 查询记录条数
        int r = userMoneyLogMapper.countByExample(userMoneyLogExample);

        TradeUser tradeUser = userMapper.selectByPrimaryKey(userMoneyLog.getUserId());


        //3. 扣减余额
        if (userMoneyLog.getMoneyLogType().intValue() == ShopCode.SHOP_USER_MONEY_PAID.getCode()){
            //余额日志中已有扣减余额的记录
            if (r>0){
                CastException.cast(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY);
                return  new Result(ShopCode.SHOP_FAIL.getSuccess(),ShopCode.SHOP_FAIL.getMessage());
            }
            //扣减用户余额
            tradeUser.setUserMoney(new BigDecimal(tradeUser.getUserMoney()).subtract(userMoneyLog.getUseMoney()).longValue());
            userMapper.updateByPrimaryKey(tradeUser);
        }

        //4.回退余额
        if (userMoneyLog.getMoneyLogType().intValue() == ShopCode.SHOP_USER_MONEY_REFUND.getCode()){
            //余额日志中没有回退余额的记录
            if (r<0){
                CastException.cast(ShopCode.SHOP_ORDER_PAY_STATUS_NO_PAY);
                return  new Result(ShopCode.SHOP_FAIL.getSuccess(),ShopCode.SHOP_FAIL.getMessage());
            }

            //防止多次退款
            TradeUserMoneyLogExample userMoneyLogExample1 = new TradeUserMoneyLogExample();
            TradeUserMoneyLogExample.Criteria criteria1 = userMoneyLogExample1.createCriteria();

            criteria1.andOrderIdEqualTo(userMoneyLog.getOrderId());
            criteria1.andUserIdEqualTo(userMoneyLog.getUserId());
            criteria1.andMoneyLogTypeEqualTo(ShopCode.SHOP_USER_MONEY_REFUND.getCode());

            //查看余额日志中退款条数
            int r2 = userMoneyLogMapper.countByExample(userMoneyLogExample1);

            //已经存在退款记录
            if (r2>0){
                CastException.cast(ShopCode.SHOP_USER_MONEY_REFUND_ALREADY);
                return  new Result(ShopCode.SHOP_FAIL.getSuccess(),ShopCode.SHOP_FAIL.getMessage());
            }

            //回退用户余额
            tradeUser.setUserMoney(new BigDecimal(tradeUser.getUserMoney()).add(userMoneyLog.getUseMoney()).longValue());
            userMapper.updateByPrimaryKey(tradeUser);

        }

        //5. 记录订单余额使用日志
        userMoneyLog.setCreateTime(new Date());
        userMoneyLogMapper.insert(userMoneyLog);

        return  new Result(ShopCode.SHOP_SUCCESS.getSuccess(),ShopCode.SHOP_SUCCESS.getMessage());
    }
}
