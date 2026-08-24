package com.xqy.sms.ai.model;

import java.util.Objects;

/** Couples a tool's public definition with the executor that implements it. */
public class ExecutableAiTool {

    private final AiToolDefinition definition;
    private final AiToolExecutor executor;

    public ExecutableAiTool(AiToolDefinition definition, AiToolExecutor executor) {
        this.definition = Objects.requireNonNull(definition, "definition must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
    }

    public AiToolDefinition definition() {
        return definition;
    }

    public AiToolExecutor executor() {
        return executor;
    }

    public AiToolDefinition getDefinition() {
        return definition;
    }

    public AiToolExecutor getExecutor() {
        return executor;
    }

    public ToolExecutionResult execute(AiTask task) {
        return executor.execute(task);
    }
}
