package com.xqy.sms.ai.api.service;

public class AiRunNotFoundException extends RuntimeException {
    public AiRunNotFoundException(String runId) { super("AI task run not found: " + runId); }
}
