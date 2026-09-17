package com.xqy.sms.system.provider;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableDubbo
@EnableScheduling
@SpringBootApplication
@MapperScan("com.xqy.sms.system.provider.mapper")
public class SystemApplication {
    public static void main(String[] args) { SpringApplication.run(SystemApplication.class, args); }
}
