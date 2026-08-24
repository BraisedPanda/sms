package com.xqy.sms.ai.model;

/** Executes a task for one tool domain. */
public interface AiToolExecutor {

    String domain();

    ToolExecutionResult execute(AiTask task);

    default String getDomain() {
        return domain();
    }
}
