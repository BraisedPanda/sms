package com.xqy.sms.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sys_role")
public class SysRole extends BaseEntity {
    private String roleCode;
    private String roleName;
    private String description;
    private String roleType;
    private String status;

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRoleType() { return roleType; }
    public void setRoleType(String roleType) { this.roleType = roleType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
