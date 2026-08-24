package com.xqy.sms.ai.model;

import java.util.List;
import java.util.Map;

/** Result returned after an AI tool has executed. */
public record ToolExecutionResult(
        String toolname,
        String type,
        List<?> items,
        Map<String, Object> summary
) {

    public String toolName() {
        return toolname;
    }
}
