package com.xqy.sms.ai.api.model;

import java.io.Serializable;
import com.xqy.sms.common.security.rpc.InternalCallContext;

public class AiChatCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long sessionId;
    private String question;
    private String alias;
    private String idempotencyKey;
    private String streamKey;
    private String tenantId;
    private InternalCallContext callerContext;

    public AiChatCommand() { }

    public AiChatCommand(Long userId, Long sessionId, String question, String alias, String idempotencyKey, String streamKey) {
        this(userId, sessionId, question, alias, idempotencyKey, streamKey, null, null);
    }

    public AiChatCommand(Long userId, Long sessionId, String question, String alias, String idempotencyKey, String streamKey, String tenantId, InternalCallContext callerContext) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.question = question;
        this.alias = alias;
        this.idempotencyKey = idempotencyKey;
        this.streamKey = streamKey;
        this.tenantId = tenantId;
        this.callerContext = callerContext;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getStreamKey() { return streamKey; }
    public void setStreamKey(String streamKey) { this.streamKey = streamKey; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public InternalCallContext getCallerContext() { return callerContext; }
    public void setCallerContext(InternalCallContext callerContext) { this.callerContext = callerContext; }

    /** Compatibility accessors retained for existing RPC callers. */
    public Long userId() { return userId; }
    public Long sessionId() { return sessionId; }
    public String question() { return question; }
    public String alias() { return alias; }
    public String idempotencyKey() { return idempotencyKey; }
    public String streamKey() { return streamKey; }
    public String tenantId() { return tenantId; }
    public InternalCallContext callerContext() { return callerContext; }
}
