package com.xqy.sms.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("sys_user_session")
public class SysUserSession extends BaseEntity {
    @TableField("tenant_id") private String tenantId;
    @TableField("user_id") private Long userId;
    private String accessToken;
    private String refreshToken;
    @TableField("access_jti") private String accessJti;
    @TableField("refresh_jti") private String refreshJti;
    private String loginIp;
    private String userAgent;
    private String deviceType;
    private LocalDateTime loginTime;
    private LocalDateTime expireTime;
    private LocalDateTime refreshExpireTime;
    private String status;
    private LocalDateTime logoutTime;
    public Long getUserId() { return userId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getAccessJti() { return accessJti; }
    public void setAccessJti(String accessJti) { this.accessJti = accessJti; }
    public String getRefreshJti() { return refreshJti; }
    public void setRefreshJti(String refreshJti) { this.refreshJti = refreshJti; }
    public String getLoginIp() { return loginIp; }
    public void setLoginIp(String loginIp) { this.loginIp = loginIp; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    public LocalDateTime getRefreshExpireTime() { return refreshExpireTime; }
    public void setRefreshExpireTime(LocalDateTime refreshExpireTime) { this.refreshExpireTime = refreshExpireTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getLogoutTime() { return logoutTime; }
    public void setLogoutTime(LocalDateTime logoutTime) { this.logoutTime = logoutTime; }
}
