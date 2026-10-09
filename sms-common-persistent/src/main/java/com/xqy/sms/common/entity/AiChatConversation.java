package com.xqy.sms.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** Durable chat conversation header. */
@TableName("ai_chat_conversation")
public class AiChatConversation extends CommonEntity {
    @TableField("tenant_id") private String tenantId;
    @TableField("user_id") private String userId;
    @TableField("session_id") private String sessionId;
    private String title;
    private String status;
    @TableField("last_message_at") private LocalDateTime lastMessageAt;
    public String getTenantId() { return tenantId; } public void setTenantId(String v) { tenantId=v; }
    public String getUserId() { return userId; } public void setUserId(String v) { userId=v; }
    public String getSessionId() { return sessionId; } public void setSessionId(String v) { sessionId=v; }
    public String getTitle() { return title; } public void setTitle(String v) { title=v; }
    public String getStatus() { return status; } public void setStatus(String v) { status=v; }
    public LocalDateTime getLastMessageAt() { return lastMessageAt; } public void setLastMessageAt(LocalDateTime v) { lastMessageAt=v; }
}
