package com.xqy.sms.ai.domain.model;

public class AiTaskRequest {

    private String userId;
    private String sessionId;
    private String question;
    /** Model alias selected for this request, for example balanced or strong. */
    private String alias;
    /** Client-supplied key used to safely retry an interrupted chat submission. */
    private String idempotencyKey;


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
