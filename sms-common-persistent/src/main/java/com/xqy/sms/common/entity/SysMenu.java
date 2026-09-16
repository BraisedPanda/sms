package com.xqy.sms.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sys_menu")
public class SysMenu extends BaseEntity {
    private Long parentId;
    private String path;
    private String routeName;
    private String component;
    private String redirect;
    private String title;
    private String icon;
    private Integer sortNo;
    private Boolean keepAlive;
    private Boolean visible;
    private Boolean hideTab;
    private Boolean fullPage;
    private String externalLink;
    private Boolean iframeFlag;
    private String activePath;
    private String status;
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public String getComponent() { return component; }
    public void setComponent(String component) { this.component = component; }
    public String getRedirect() { return redirect; }
    public void setRedirect(String redirect) { this.redirect = redirect; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public Boolean getKeepAlive() { return keepAlive; }
    public void setKeepAlive(Boolean keepAlive) { this.keepAlive = keepAlive; }
    public Boolean getVisible() { return visible; }
    public void setVisible(Boolean visible) { this.visible = visible; }
    public Boolean getHideTab() { return hideTab; }
    public void setHideTab(Boolean hideTab) { this.hideTab = hideTab; }
    public Boolean getFullPage() { return fullPage; }
    public void setFullPage(Boolean fullPage) { this.fullPage = fullPage; }
    public String getExternalLink() { return externalLink; }
    public void setExternalLink(String externalLink) { this.externalLink = externalLink; }
    public Boolean getIframeFlag() { return iframeFlag; }
    public void setIframeFlag(Boolean iframeFlag) { this.iframeFlag = iframeFlag; }
    public String getActivePath() { return activePath; }
    public void setActivePath(String activePath) { this.activePath = activePath; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
