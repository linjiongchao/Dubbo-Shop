package com.lin.shop;


import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import com.lin.shop.util.IDWorker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDubbo(scanBasePackages = "com.lin.shop.service.impl")
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class,args);

    }

    @Bean
    public IDWorker idWorker(){
        return new IDWorker(1,1);
    }

}


