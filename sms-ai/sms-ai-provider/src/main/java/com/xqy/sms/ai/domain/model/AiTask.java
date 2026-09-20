package com.xqy.sms.ai.domain.model;

import java.util.ArrayList;
import java.util.List;

/** A request to execute one registered AI tool. */
public class AiTask {

    private String requestId;
    private String tenantId;
    private String userId;
    private String sessionId;
    private String domain;
    private String toolName;
    private String reason;
    private String subQuestion;
    private QueryCriteria query;
    private List<String> missingArgs = new ArrayList<>();
    /** Optional pre-computed embedding supplied by the planner/vectorizer. */
    private List<Float> embedding = new ArrayList<>();

    public AiTask() {
    }

    public String getRequestId() {
        return requestId;
    }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public AiTask(String domain, String toolName, String reason, QueryCriteria query,
                  List<String> missingArgs) {
        this.domain = domain;
        this.toolName = toolName;
        this.reason = reason;
        this.query = query;
        this.missingArgs = missingArgs;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getToolName() {
        return toolName;
    }

    public String getSubQuestion() {
        return subQuestion;
    }

    public void setSubQuestion(String subQuestion) {
        this.subQuestion = subQuestion;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public QueryCriteria getQuery() {
        return query;
    }

    public void setQuery(QueryCriteria query) {
        this.query = query;
    }

    public List<String> getMissingArgs() {
        return missingArgs;
    }

    public void setMissingArgs(List<String> missingArgs) {
        this.missingArgs = missingArgs;
    }

    /** Backward-compatible aliases for callers using the original misspelling. */
    public List<String> getMissngArgs() {
        return missingArgs;
    }

    public void setMissngArgs(List<String> missingArgs) {
        this.missingArgs = missingArgs;
    }

    public List<Float> getEmbedding() { return embedding; }
    public void setEmbedding(List<Float> embedding) { this.embedding = embedding; }
}
