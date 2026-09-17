package com.xqy.sms.ai.domain.model;

/** Executes a task for one tool domain. */
public interface AiToolExecutor {

    String domain();

    AiTaskResult execute(AiTask task);

}
