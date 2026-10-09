package com.xqy.sms.common.security.rpc;

import java.io.Serializable;

public class InternalCallContext implements Serializable {
    private static final long serialVersionUID = 1L;

    private String callerService;
    private String tenantId;
    private Long userId;
    private Long sessionId;
    private String requestId;
    private long issuedAtEpochSecond;
    private String signature;

    public InternalCallContext() { }

    public InternalCallContext(String callerService, String tenantId, Long userId, Long sessionId, String requestId, long issuedAtEpochSecond, String signature) {
        this.callerService = callerService;
        this.tenantId = tenantId;
        this.userId = userId;
        this.sessionId = sessionId;
        this.requestId = requestId;
        this.issuedAtEpochSecond = issuedAtEpochSecond;
        this.signature = signature;
    }

    public String getCallerService() { return callerService; }
    public void setCallerService(String callerService) { this.callerService = callerService; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public long getIssuedAtEpochSecond() { return issuedAtEpochSecond; }
    public void setIssuedAtEpochSecond(long issuedAtEpochSecond) { this.issuedAtEpochSecond = issuedAtEpochSecond; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    /** Compatibility accessors retained for existing RPC callers. */
    public String callerService() { return callerService; }
    public String tenantId() { return tenantId; }
    public Long userId() { return userId; }
    public Long sessionId() { return sessionId; }
    public String requestId() { return requestId; }
    public long issuedAtEpochSecond() { return issuedAtEpochSecond; }
    public String signature() { return signature; }
}
