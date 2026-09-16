package com.xqy.sms.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sys_role_button")
public class SysRoleButton extends BaseEntity {
    @TableField("role_id") private Long roleId;
    @TableField("button_id") private Long buttonId;
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public Long getButtonId() { return buttonId; }
    public void setButtonId(Long buttonId) { this.buttonId = buttonId; }
}
