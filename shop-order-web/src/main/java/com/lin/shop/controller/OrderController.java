package com.lin.shop.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.lin.shop.pojo.TradeOrder;
import com.lin.shop.service.IOrderService;
import com.lin.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "/order")
public class OrderController {

    @Reference
    private IOrderService orderService;

    @RequestMapping(value = "/confirmOrder", method = RequestMethod.POST)
    Result confirmOrder(TradeOrder order){
        System.out.println(JSON.toJSONString(order));
        return orderService.confirmOrder(order);

        //127.0.0.1:8888/order/confirmOrder
//        {
//            "goodsId": "345959443973935104",
//                "userId": "345963634385633280",
//                "couponId": "345988230098857984",
//                "address": "北京市",
//                "goodsNumber": "1",
//                "goodsPrice": "5000",
//                "shippingFee": "0",
//                "orderAmount": "5000",
//                "moneyPaid": "100"
//        }
    }



}
