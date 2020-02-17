package com.lin.shop.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.lin.shop.pojo.TradePay;
import com.lin.shop.service.IPayService;
import com.lin.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = "/pay")
public class PayController {

    @Reference
    private IPayService payService;

    @RequestMapping(value = "/createPayment",method = RequestMethod.POST)
    public Result createPayment(TradePay tradePay){
        return payService.createPayment(tradePay);


        //127.0.0.1:8889/pay/createPayment
//        {
//            "orderId": "426805543001264128",
//                "payAmount": "880"
//        }

    }
    @RequestMapping(value = "/callbackPayments",method = RequestMethod.POST)
    public Result callbackPayment(TradePay tradePay){
        return payService.callbackPayment(tradePay);

        //127.0.0.1:8889/pay/callbackPayments
//        {
//            "orderId": "426805543001264128",
//                "isPaid": "2",
//                "payId": "426808259014434816"
//        }
    }
}
