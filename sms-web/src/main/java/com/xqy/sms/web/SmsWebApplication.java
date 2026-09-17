package com.xqy.sms.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;

@EnableDubbo
@SpringBootApplication
@MapperScan("com.xqy.sms.web.infrastructure.persistence.mapper")
public class SmsWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmsWebApplication.class, args);
    }

}
