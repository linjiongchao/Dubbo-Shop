package com.lin.shop;

import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubbo
public class CouponApplication {
    public static void main(String[] args) {
        SpringApplication.run(CouponApplication.class,args);
    }
}
