package com.xqy.sms.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/** Durable user or assistant message belonging to a chat conversation. */
@TableName("ai_chat_message")
public class AiChatMessage extends CommonEntity {
    @TableField("conversation_id") private Long conversationId;
    @TableField("tenant_id") private String tenantId;
    @TableField("user_id") private String userId;
    @TableField("session_id") private String sessionId;
    private String role;
    private String content;
    @TableField("run_id") private String runId;
    @TableField("sequence_no") private Integer sequenceNo;
    public Long getConversationId() { return conversationId; } public void setConversationId(Long v) { conversationId=v; }
    public String getTenantId() { return tenantId; } public void setTenantId(String v) { tenantId=v; }
    public String getUserId() { return userId; } public void setUserId(String v) { userId=v; }
    public String getSessionId() { return sessionId; } public void setSessionId(String v) { sessionId=v; }
    public String getRole() { return role; } public void setRole(String v) { role=v; }
    public String getContent() { return content; } public void setContent(String v) { content=v; }
    public String getRunId() { return runId; } public void setRunId(String v) { runId=v; }
    public Integer getSequenceNo() { return sequenceNo; } public void setSequenceNo(Integer v) { sequenceNo=v; }
}
