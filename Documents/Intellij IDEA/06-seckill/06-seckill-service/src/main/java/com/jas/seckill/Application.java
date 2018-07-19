package com.jas.seckill;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableDubboConfiguration
@SpringBootApplication
@EnableTransactionManagement //开启事务管理
public class Application {

    public static void main(String[] args) {

        SpringApplication.run(Application.class, args);
    }
}
