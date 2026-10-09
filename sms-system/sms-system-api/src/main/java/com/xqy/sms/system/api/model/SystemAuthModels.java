package com.xqy.sms.system.api.model;

import java.io.Serializable;
import java.util.List;

/** Hessian-friendly system authentication DTOs. */
public final class SystemAuthModels {
    private SystemAuthModels() { }

    public static class TokenPair implements Serializable {
        private static final long serialVersionUID = 1L;

        private String token;
        private String refreshToken;

        public TokenPair() { }

        public TokenPair(String token, String refreshToken) {
            this.token = token;
            this.refreshToken = refreshToken;
        }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

        /** Compatibility accessors retained for existing RPC callers. */
        public String token() { return token; }
        public String refreshToken() { return refreshToken; }
    }

    public static class Principal implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long userId;
        private Long sessionId;
        private String tokenId;
        private String tenantId;
        private List<String> roles;
        private List<String> authorities;

        public Principal() { }

        public Principal(Long userId, Long sessionId, String tokenId, String tenantId, List<String> roles, List<String> authorities) {
            this.userId = userId;
            this.sessionId = sessionId;
            this.tokenId = tokenId;
            this.tenantId = tenantId;
            this.roles = roles;
            this.authorities = authorities;
        }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
        public String getTokenId() { return tokenId; }
        public void setTokenId(String tokenId) { this.tokenId = tokenId; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        public List<String> getAuthorities() { return authorities; }
        public void setAuthorities(List<String> authorities) { this.authorities = authorities; }

        /** Compatibility accessors retained for existing RPC callers. */
        public Long userId() { return userId; }
        public Long sessionId() { return sessionId; }
        public String tokenId() { return tokenId; }
        public String tenantId() { return tenantId; }
        public List<String> roles() { return roles; }
        public List<String> authorities() { return authorities; }
    }

    public static class UserInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private List<String> buttons;
        private List<String> roles;
        private Long userId;
        private String userName;
        private String email;
        private String avatar;

        public UserInfo() { }

        public UserInfo(List<String> buttons, List<String> roles, Long userId, String userName, String email, String avatar) {
            this.buttons = buttons;
            this.roles = roles;
            this.userId = userId;
            this.userName = userName;
            this.email = email;
            this.avatar = avatar;
        }

        public List<String> getButtons() { return buttons; }
        public void setButtons(List<String> buttons) { this.buttons = buttons; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }

        /** Compatibility accessors retained for existing RPC callers. */
        public List<String> buttons() { return buttons; }
        public List<String> roles() { return roles; }
        public Long userId() { return userId; }
        public String userName() { return userName; }
        public String email() { return email; }
        public String avatar() { return avatar; }
    }

    public static class Menu implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long id;
        private Long parentId;
        private String path;
        private String name;
        private String component;
        private String redirect;
        private String title;
        private String icon;
        private Integer sort;
        private Boolean keepAlive;
        private Boolean visible;
        private Boolean hideTab;
        private Boolean fullPage;
        private String link;
        private Boolean iframe;
        private List<Button> authList;
        private String activePath;

        public Menu() { }

        public Menu(Long id, Long parentId, String path, String name, String component, String redirect, String title, String icon, Integer sort, Boolean keepAlive, Boolean visible, Boolean hideTab, Boolean fullPage, String link, Boolean iframe, List<Button> authList) {
            this.id = id;
            this.parentId = parentId;
            this.path = path;
            this.name = name;
            this.component = component;
            this.redirect = redirect;
            this.title = title;
            this.icon = icon;
            this.sort = sort;
            this.keepAlive = keepAlive;
            this.visible = visible;
            this.hideTab = hideTab;
            this.fullPage = fullPage;
            this.link = link;
            this.iframe = iframe;
            this.authList = authList;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getParentId() { return parentId; }
        public void setParentId(Long parentId) { this.parentId = parentId; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getComponent() { return component; }
        public void setComponent(String component) { this.component = component; }
        public String getRedirect() { return redirect; }
        public void setRedirect(String redirect) { this.redirect = redirect; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public Integer getSort() { return sort; }
        public void setSort(Integer sort) { this.sort = sort; }
        public Boolean getKeepAlive() { return keepAlive; }
        public void setKeepAlive(Boolean keepAlive) { this.keepAlive = keepAlive; }
        public Boolean getVisible() { return visible; }
        public void setVisible(Boolean visible) { this.visible = visible; }
        public Boolean getHideTab() { return hideTab; }
        public void setHideTab(Boolean hideTab) { this.hideTab = hideTab; }
        public Boolean getFullPage() { return fullPage; }
        public void setFullPage(Boolean fullPage) { this.fullPage = fullPage; }
        public String getLink() { return link; }
        public void setLink(String link) { this.link = link; }
        public Boolean getIframe() { return iframe; }
        public void setIframe(Boolean iframe) { this.iframe = iframe; }
        public List<Button> getAuthList() { return authList; }
        public void setAuthList(List<Button> authList) { this.authList = authList; }
        public String getActivePath() { return activePath; }
        public void setActivePath(String activePath) { this.activePath = activePath; }

        /** Compatibility accessors retained for existing RPC callers. */
        public Long id() { return id; }
        public Long parentId() { return parentId; }
        public String path() { return path; }
        public String name() { return name; }
        public String component() { return component; }
        public String redirect() { return redirect; }
        public String title() { return title; }
        public String icon() { return icon; }
        public Integer sort() { return sort; }
        public Boolean keepAlive() { return keepAlive; }
        public Boolean visible() { return visible; }
        public Boolean hideTab() { return hideTab; }
        public Boolean fullPage() { return fullPage; }
        public String link() { return link; }
        public Boolean iframe() { return iframe; }
        public List<Button> authList() { return authList; }
        public String activePath() { return activePath; }
    }

    public static class Button implements Serializable {
        private static final long serialVersionUID = 1L;

        private String title;
        private String authMark;

        public Button() { }

        public Button(String title, String authMark) {
            this.title = title;
            this.authMark = authMark;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getAuthMark() { return authMark; }
        public void setAuthMark(String authMark) { this.authMark = authMark; }

        /** Compatibility accessors retained for existing RPC callers. */
        public String title() { return title; }
        public String authMark() { return authMark; }
    }

    public static class UserPage implements Serializable {
        private static final long serialVersionUID = 1L;

        private List<User> records;
        private long current;
        private long size;
        private long total;

        public UserPage() { }

        public UserPage(List<User> records, long current, long size, long total) {
            this.records = records;
            this.current = current;
            this.size = size;
            this.total = total;
        }

        public List<User> getRecords() { return records; }
        public void setRecords(List<User> records) { this.records = records; }
        public long getCurrent() { return current; }
        public void setCurrent(long current) { this.current = current; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }

        /** Compatibility accessors retained for existing RPC callers. */
        public List<User> records() { return records; }
        public long current() { return current; }
        public long size() { return size; }
        public long total() { return total; }
    }

    public static class User implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long id;
        private String userName;
        private String userGender;
        private String nickName;
        private String userPhone;
        private String userEmail;
        private String avatar;
        private String status;
        private List<String> userRoles;
        private String createBy;
        private String createTime;
        private String updateBy;
        private String updateTime;

        public User() { }

        public User(Long id, String userName, String userGender, String nickName, String userPhone, String userEmail, String avatar, String status, List<String> userRoles, String createBy, String createTime, String updateBy, String updateTime) {
            this.id = id;
            this.userName = userName;
            this.userGender = userGender;
            this.nickName = nickName;
            this.userPhone = userPhone;
            this.userEmail = userEmail;
            this.avatar = avatar;
            this.status = status;
            this.userRoles = userRoles;
            this.createBy = createBy;
            this.createTime = createTime;
            this.updateBy = updateBy;
            this.updateTime = updateTime;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getUserGender() { return userGender; }
        public void setUserGender(String userGender) { this.userGender = userGender; }
        public String getNickName() { return nickName; }
        public void setNickName(String nickName) { this.nickName = nickName; }
        public String getUserPhone() { return userPhone; }
        public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
        public String getUserEmail() { return userEmail; }
        public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public List<String> getUserRoles() { return userRoles; }
        public void setUserRoles(List<String> userRoles) { this.userRoles = userRoles; }
        public String getCreateBy() { return createBy; }
        public void setCreateBy(String createBy) { this.createBy = createBy; }
        public String getCreateTime() { return createTime; }
        public void setCreateTime(String createTime) { this.createTime = createTime; }
        public String getUpdateBy() { return updateBy; }
        public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
        public String getUpdateTime() { return updateTime; }
        public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }

        /** Compatibility accessors retained for existing RPC callers. */
        public Long id() { return id; }
        public String userName() { return userName; }
        public String userGender() { return userGender; }
        public String nickName() { return nickName; }
        public String userPhone() { return userPhone; }
        public String userEmail() { return userEmail; }
        public String avatar() { return avatar; }
        public String status() { return status; }
        public List<String> userRoles() { return userRoles; }
        public String createBy() { return createBy; }
        public String createTime() { return createTime; }
        public String updateBy() { return updateBy; }
        public String updateTime() { return updateTime; }
    }
}
