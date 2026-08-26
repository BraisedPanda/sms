package com.xqy.sms.ai.model;

import com.xqy.sms.common.entity.AiToolDefinition;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Registry used to look up and execute tools by domain and tool name. */
public class AiToolRegister implements AiToolDefinitionProvider {

    private final Map<ToolKey, ExecutableAiTool> tools = new ConcurrentHashMap<>();

    public AiToolRegister() {
    }

    public AiToolRegister(Collection<? extends ExecutableAiTool> tools) {
        if (tools != null) {
            tools.forEach(this::register);
        }
    }

    public void register(ExecutableAiTool tool) {
        Objects.requireNonNull(tool, "tool must not be null");
        AiToolDefinition definition = tool.definition();
        ToolKey key = key(definition.getDomain(), definition.getToolName());
        if (tools.putIfAbsent(key, tool) != null) {
            throw new IllegalArgumentException("Tool already registered: " + key);
        }
    }

    public void register(AiToolDefinition definition, AiToolExecutor executor) {
        register(new ExecutableAiTool(definition, executor));
    }

    public Optional<ExecutableAiTool> find(String domain, String toolName) {
        return Optional.ofNullable(tools.get(key(domain, toolName)));
    }

    public ToolExecutionResult execute(AiTask task) {
        Objects.requireNonNull(task, "task must not be null");
        ExecutableAiTool tool = find(task.getDomain(), task.getToolName())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No tool registered for domain='" + task.getDomain() + "', tool='" + task.getToolName() + "'"));
        return tool.execute(task);
    }

    @Override
    public List<AiToolDefinition> definitions() {
        return tools.values().stream().map(ExecutableAiTool::definition).toList();
    }

    public Collection<ExecutableAiTool> tools() {
        return List.copyOf(tools.values());
    }

    private static ToolKey key(String domain, String toolName) {
        return new ToolKey(Objects.requireNonNull(domain, "domain must not be null"),
                Objects.requireNonNull(toolName, "toolName must not be null"));
    }

    private record ToolKey(String domain, String toolName) {
    }
}
