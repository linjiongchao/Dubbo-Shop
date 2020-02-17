package com.lin.shop;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lin.shop.constant.ShopCode;
import com.lin.shop.pojo.TradeOrder;
import com.lin.shop.pojo.TradePay;
import com.lin.shop.service.IOrderService;
import com.lin.shop.service.IPayService;
import com.lin.shop.service.IUserService;
import com.lin.util.Result;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.math.BigDecimal;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = PayApplication.class)
public class PayServiceTest {

    @Autowired
    private IPayService payService;

    @Test
    public void createPayment(){

        Long orderId = 426104490471591936L;


        TradePay tradePay = new TradePay();
        tradePay.setOrderId(orderId);
        tradePay.setPayAmount(new BigDecimal(880));

        Result result = payService.createPayment(tradePay);
        System.out.println("result:" + JSON.toJSONString(result));
    }

    @Test
    public void  callbackPayment() throws IOException {
        Long orderId = 426104490471591936L;
        Long payId = 426449319260987392L;

        TradePay tradePay = new TradePay();
        tradePay.setOrderId(orderId);
        tradePay.setPayId(payId);
        tradePay.setIsPaid(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY.getCode());

        Result result = payService.callbackPayment(tradePay);
        System.out.println("result:" + JSON.toJSONString(result));

        System.in.read();
    }


}
