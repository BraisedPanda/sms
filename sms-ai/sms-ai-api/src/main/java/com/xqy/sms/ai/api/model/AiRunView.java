package com.xqy.sms.ai.api.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/** User-safe run projection; prompts, plans, tool inputs and internal errors are intentionally omitted. */
public record AiRunView(String runId, String status, Integer currentStepNo,
                        LocalDateTime startTime, LocalDateTime finishTime) implements Serializable { }
