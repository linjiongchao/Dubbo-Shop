package com.lin.shop;


import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import com.lin.shop.util.IDWorker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;


@SpringBootApplication
@EnableDubbo
public class PayApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayApplication.class,args);
    }


    @Bean
    public IDWorker idWorker(){
        return new IDWorker(1,2);
    }

    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor(){

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setKeepAliveSeconds(60);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Pool-A");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();

        return executor;
    }


}


