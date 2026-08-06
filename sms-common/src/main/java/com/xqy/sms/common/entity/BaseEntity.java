package com.xqy.sms.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Id;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
public class BaseEntity {

    @Id
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("sys_creator")
    private String sysCreator;

    @TableField("sys_modifier")
    private String sysModifier;

    @TableField(value = "sys_create_time", fill = FieldFill.INSERT)
    private LocalDateTime sysCreateTime;

    @TableField(value = "sys_update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime sysUpdateTime;


}
