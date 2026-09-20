package com.xqy.sms.system.api.model;

import java.io.Serializable;
import java.util.List;

public final class SystemAuthModels {
    private SystemAuthModels() { }
    public record TokenPair(String token, String refreshToken) implements Serializable { }
    public record Principal(Long userId, Long sessionId, String tokenId, String tenantId, List<String> roles,
                            List<String> authorities) implements Serializable { }
    public record UserInfo(List<String> buttons, List<String> roles, Long userId, String userName,
                           String email, String avatar) implements Serializable { }
    public record Menu(Long id, Long parentId, String path, String name, String component, String redirect,
                       String title, String icon, Integer sort, Boolean keepAlive, Boolean visible,
                       Boolean hideTab, Boolean fullPage, String link, Boolean iframe,
                       List<Button> authList) implements Serializable { }
    public record Button(String title, String authMark) implements Serializable { }
    public record UserPage(List<User> records, long current, long size, long total) implements Serializable { }
    public record User(Long id, String userName, String userGender, String nickName, String userPhone,
                       String userEmail, String avatar, String status, List<String> userRoles,
                       String createBy, String createTime, String updateBy, String updateTime) implements Serializable { }
}
