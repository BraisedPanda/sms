package com.xqy.sms.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;

public class BaseEntity {

    /** MyBatis-Plus ASSIGN_ID uses its built-in Snowflake identifier generator. */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("sys_creator")
    private String sysCreator;

    @TableField("sys_modifier")
    private String sysModifier;

    @TableField(value = "sys_create_time", fill = FieldFill.INSERT)
    private LocalDateTime sysCreateTime;

    @TableField(value = "sys_update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime sysUpdateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSysCreator() {
        return sysCreator;
    }

    public void setSysCreator(String sysCreator) {
        this.sysCreator = sysCreator;
    }

    public String getSysModifier() {
        return sysModifier;
    }

    public void setSysModifier(String sysModifier) {
        this.sysModifier = sysModifier;
    }

    public LocalDateTime getSysCreateTime() {
        return sysCreateTime;
    }

    public void setSysCreateTime(LocalDateTime sysCreateTime) {
        this.sysCreateTime = sysCreateTime;
    }

    public LocalDateTime getSysUpdateTime() {
        return sysUpdateTime;
    }

    public void setSysUpdateTime(LocalDateTime sysUpdateTime) {
        this.sysUpdateTime = sysUpdateTime;
    }
}
