package com.lin.shop;

import com.alibaba.dubbo.config.annotation.Reference;
import com.lin.shop.pojo.TradeOrder;
import com.lin.shop.service.IOrderService;
import com.lin.shop.service.IUserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.security.RunAs;
import java.math.BigDecimal;
import java.util.Scanner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = OrderApplication.class)
public class OrderServiceTest {

    @Autowired
    private IOrderService orderService;

    @Reference
    private IUserService userService;


    @Test
    public void confirmOrder(){
        Long couponId = 345988230098857984L;
        Long goodsId = 345959443973935104L;
        Long userId = 345963634385633280L;

        TradeOrder order = new TradeOrder();
        order.setGoodsId(goodsId);
        order.setUserId(userId);
        order.setCouponId(couponId);
        order.setAddress("北京");
        order.setGoodsNumber(1);
        order.setGoodsPrice(new BigDecimal(5000));
        order.setShippingFee(BigDecimal.ZERO);
        order.setOrderAmount(new BigDecimal(5000));
        order.setMoneyPaid(new BigDecimal(100));

        System.out.println("orderService:" + orderService);
        orderService.confirmOrder(order);




    }

    @Test
    public void find(){
        System.out.println(userService);
        userService.findOne(345963634385633280L);
    }
}
