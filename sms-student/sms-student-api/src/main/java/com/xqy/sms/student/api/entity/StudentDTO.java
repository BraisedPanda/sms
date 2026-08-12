package com.xqy.sms.student.api.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学生实体
 */
@Data
public class StudentDTO {

    private Long id;

    private String studentNo;

    private String name;

    private Integer age;

    private String gender;

    private LocalDate birthday;

    private String email;

    private String phone;

    private LocalDateTime enrolledAt;

}


