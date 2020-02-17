package com.lin.shop;

import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubbo
public class PayWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayWebApplication.class,args);
    }
}
