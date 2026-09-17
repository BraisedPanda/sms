package com.xqy.sms.ai.domain.model;

/** States shared by persistent task runs and steps. */
public final class AiTaskRunStatus {
    public static final String PENDING = "PENDING";
    public static final String PLANNING = "PLANNING";
    public static final String RUNNING = "RUNNING";
    public static final String RETRY_WAIT = "RETRY_WAIT";
    public static final String COMPOSING = "COMPOSING";
    public static final String SUCCEEDED = "SUCCEEDED";
    public static final String FAILED = "FAILED";
    public static final String CANCEL_REQUESTED = "CANCEL_REQUESTED";
    public static final String CANCELLED = "CANCELLED";

    private AiTaskRunStatus() {
    }

    public static boolean isTerminal(String status) {
        return SUCCEEDED.equals(status) || FAILED.equals(status) || CANCELLED.equals(status);
    }
}
