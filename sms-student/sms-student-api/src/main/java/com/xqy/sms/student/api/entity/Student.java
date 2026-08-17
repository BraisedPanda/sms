package com.xqy.sms.student.api.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xqy.sms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学生实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "student")
@TableName("student")
public class Student extends BaseEntity implements Serializable {

    @TableField("student_no")
    @Column(name = "student_no")
    private String studentNo;

    @Column(name = "name")
    private String name;

    @Column(name = "age")
    private Integer age;

    @Column(name = "gender")
    private String gender;

    @Column(name = "birthday")
    private LocalDate birthday;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @TableField("enrolled_at")
    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;
}
