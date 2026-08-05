package com.xqy.sms.student;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.xqy.sms.student.mapper")
@SpringBootApplication
public class SmsStudentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmsStudentApplication.class, args);
    }

}
