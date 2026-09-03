package com.xqy.sms.ai.model;

import com.xqy.sms.common.entity.AiToolDefinition;
import com.xqy.sms.ai.service.log.AiToolExecuteLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Registry of tool definitions and domain-level executors. */
@Component
public class AiToolRegistry {

    private final Map<String, AiToolDefinition> definitionByToolName = new ConcurrentHashMap<>();
    private final Map<String, AiToolExecutor> executorsByDomain = new ConcurrentHashMap<>();
    private AiToolExecuteLogService toolExecuteLogService;

    public AiToolRegistry() {
        this.toolExecuteLogService = null;
    }

    /**
     * Spring constructor. Loads definitions and executors at startup, which
     * keeps registry initialization in one place instead of a separate
     * configuration class.
     */
    public AiToolRegistry(AiToolDefinitionProvider definitionProvider,
                          List<AiToolExecutor> executors) {
        this(definitionProvider == null ? null : definitionProvider.getDefinitions(), executors);
    }

    @Autowired
    public AiToolRegistry(AiToolDefinitionProvider definitionProvider,
                          List<AiToolExecutor> executors,
                          AiToolExecuteLogService toolExecuteLogService) {
        this(definitionProvider == null ? null : definitionProvider.getDefinitions(), executors);
        this.toolExecuteLogService = toolExecuteLogService;
    }

    public AiToolRegistry(Collection<AiToolDefinition> definitions,
                          Collection<? extends AiToolExecutor> executors) {
        this.toolExecuteLogService = null;
        if (definitions == null) {
            return;
        }
        for (AiToolDefinition definition : definitions) {
            if (definition == null) {
                continue;
            }
            AiToolExecutor executor = findExecutor(executors, definition.getDomain());
            if (executor == null) {
                throw new IllegalArgumentException("No executor registered for domain='"
                        + definition.getDomain() + "'");
            }
            register(definition, executor);
        }
    }

    /** Register one public definition and its domain executor. */
    public void register(AiToolDefinition definition, AiToolExecutor executor) {
        Objects.requireNonNull(definition, "definition must not be null");
        Objects.requireNonNull(executor, "executor must not be null");
        String toolName = required(definition.getToolName(), "definition.toolName");
        String definitionDomain = required(definition.getDomain(), "definition.domain");
        String executorDomain = required(executor.domain(), "executor.domain");
        if (!definitionDomain.equalsIgnoreCase(executorDomain)) {
            throw new IllegalArgumentException("Definition domain '" + definitionDomain
                    + "' does not match executor domain '" + executorDomain + "'");
        }

        String normalizedToolName = normalize(toolName);
        String normalizedDomain = normalize(definitionDomain);
        AiToolDefinition previousDefinition = definitionByToolName.putIfAbsent(normalizedToolName, definition);
        if (previousDefinition != null && previousDefinition != definition) {
            throw new IllegalArgumentException("Tool already registered: " + toolName);
        }
        AiToolExecutor previousExecutor = executorsByDomain.putIfAbsent(normalizedDomain, executor);
        if (previousExecutor != null && previousExecutor != executor) {
            if (previousDefinition == null) {
                definitionByToolName.remove(normalizedToolName, definition);
            }
            throw new IllegalArgumentException("A different executor is already registered for domain: "
                    + definitionDomain);
        }
    }

    /** Find a definition by public tool name. */
    public Optional<AiToolDefinition> find(String toolName) {
        return Optional.ofNullable(toolName == null ? null : definitionByToolName.get(normalize(toolName)));
    }

    /** Find the executor responsible for a domain/tool pair. */
    public Optional<AiToolExecutor> find(String domain, String toolName) {
        if (domain == null || toolName == null) {
            return Optional.empty();
        }
        AiToolDefinition definition = definitionByToolName.get(normalize(toolName));
        if (definition == null || !normalize(domain).equals(normalize(definition.getDomain()))) {
            return Optional.empty();
        }
        return Optional.ofNullable(executorsByDomain.get(normalize(domain)));
    }

    public Optional<AiToolExecutor> findExecutor(String domain) {
        return Optional.ofNullable(domain == null ? null : executorsByDomain.get(normalize(domain)));
    }

    public Optional<AiToolDefinition> findDefinition(String toolName) {
        return find(toolName);
    }

    /** Execute a task through the executor registered for its domain. */
    public AiTaskResult execute(AiTask task) {
        Objects.requireNonNull(task, "task must not be null");
        String domain = required(task.getDomain(), "task.domain");
        String toolName = required(task.getToolName(), "task.toolName");
        AiToolDefinition definition = find(toolName)
                .orElseThrow(() -> new IllegalArgumentException("No tool registered: " + toolName));
        if (!domain.equalsIgnoreCase(definition.getDomain())) {
            throw new IllegalArgumentException("Tool '" + toolName + "' does not belong to domain '" + domain + "'");
        }
        AiToolExecutor executor = findExecutor(domain)
                .orElseThrow(() -> new IllegalArgumentException("No executor registered for domain='" + domain + "'"));
        AiToolExecuteLogService logger = toolExecuteLogService;
        com.xqy.sms.ai.model.AiToolExecuteLog executionLog = logger == null ? null : logger.start(task, null);
        try {
            AiTaskResult result = executor.execute(task);
            if (logger != null && executionLog != null) {
                logger.success(executionLog.getToolExecuteId(), result);
            }
            if (result != null && executionLog != null && result.getToolCallId() == null) {
                result.setToolCallId(executionLog.getToolExecuteId());
            }
            return result;
        } catch (RuntimeException | Error error) {
            if (logger != null && executionLog != null) {
                logger.fail(executionLog.getToolExecuteId(), error);
            }
            throw error;
        }
    }

    public List<AiToolDefinition> definitions() {
        return List.copyOf(definitionByToolName.values());
    }

    public Collection<AiToolExecutor> executors() {
        return List.copyOf(executorsByDomain.values());
    }

    public Map<String, AiToolDefinition> getDefinitionByToolName() {
        return Map.copyOf(definitionByToolName);
    }

    public Map<String, AiToolExecutor> getExecutorsByDomain() {
        return Map.copyOf(executorsByDomain);
    }

    private static AiToolExecutor findExecutor(Collection<? extends AiToolExecutor> executors, String domain) {
        if (executors == null || domain == null) {
            return null;
        }
        for (AiToolExecutor executor : executors) {
            if (executor != null && domain.equalsIgnoreCase(executor.domain())) {
                return executor;
            }
        }
        return null;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
