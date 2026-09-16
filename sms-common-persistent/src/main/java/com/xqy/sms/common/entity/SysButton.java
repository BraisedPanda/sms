package com.xqy.sms.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sys_button")
public class SysButton extends BaseEntity {
    @TableField("menu_id") private Long menuId;
    private String buttonName;
    private String authRemark;
    private String description;
    private Integer sortNo;
    private String status;
    public Long getMenuId() { return menuId; }
    public void setMenuId(Long menuId) { this.menuId = menuId; }
    public String getButtonName() { return buttonName; }
    public void setButtonName(String buttonName) { this.buttonName = buttonName; }
    public String getAuthRemark() { return authRemark; }
    public void setAuthRemark(String authRemark) { this.authRemark = authRemark; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
