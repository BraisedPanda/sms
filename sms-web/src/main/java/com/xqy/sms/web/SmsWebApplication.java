package com.xqy.sms.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;

@EnableDubbo
@SpringBootApplication
public class SmsWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmsWebApplication.class, args);
    }

}
