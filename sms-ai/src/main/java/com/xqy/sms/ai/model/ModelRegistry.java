package com.xqy.sms.ai.model;

import com.xqy.sms.ai.service.model.AiModelDefinitionService;
import com.xqy.sms.common.entity.AiModelDefinition;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Runtime registry of enabled model definitions and their provider handles. */
@Component
public class ModelRegistry {

    private static final Logger log = LoggerFactory.getLogger(ModelRegistry.class);
    private final AiModelDefinitionService definitionService;
    private final Map<String, AiModelDefinition> definitionsByAlias = new ConcurrentHashMap<>();
    private final Map<String, ModelHandle> handlesByAlias = new ConcurrentHashMap<>();
    private final Map<String, ModelAdapter> adaptersByProvider = new ConcurrentHashMap<>();

    @Autowired
    public ModelRegistry(AiModelDefinitionService definitionService, List<ModelAdapter> adapters) {
        this.definitionService = definitionService;
        if (adapters != null) {
            adapters.stream().filter(Objects::nonNull)
                    .forEach(adapter -> adaptersByProvider.put(normalize(adapter.provider()), adapter));
        }
        if (definitionService != null) {
            registerAll(definitionService.definitions());
        }
    }

    /** Constructor useful for focused unit tests without Spring or a database. */
    public ModelRegistry(Collection<AiModelDefinition> definitions, Collection<ModelAdapter> adapters) {
        this.definitionService = null;
        if (adapters != null) {
            adapters.stream().filter(Objects::nonNull)
                    .forEach(adapter -> adaptersByProvider.put(normalize(adapter.provider()), adapter));
        }
        registerAll(definitions);
    }

    @PostConstruct
    public void initialize() {
        if (definitionService != null && handlesByAlias.isEmpty()) {
            registerAll(definitionService.definitions());
        }
        if (handlesByAlias.isEmpty()) {
            log.warn("No enabled AI model definition was registered");
        }
    }

    public synchronized void reload() {
        definitionsByAlias.clear();
        handlesByAlias.clear();
        if (definitionService != null) {
            registerAll(definitionService.definitions());
        }
    }

    public void register(AiModelDefinition definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        if (!isEnabled(definition.getEnabled())) {
            return;
        }
        String alias = required(definition.getAlias(), "definition.alias");
        ModelAdapter adapter = findAdapter(definition.getProvider())
                .orElseThrow(() -> new IllegalArgumentException("No adapter for provider: "
                        + definition.getProvider()));
        ModelHandle handle = adapter.create(definition);
        definitionsByAlias.put(normalize(alias), definition);
        handlesByAlias.put(normalize(alias), handle);
    }

    public ModelHandle require(String alias) {
        String requested = required(alias, "alias");
        return resolve(requested, new ArrayList<>());
    }

    public Optional<ModelHandle> find(String alias) {
        if (alias == null || alias.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(require(alias));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public String modelName(String alias) {
        AiModelDefinition definition = findDefinition(alias)
                .orElseThrow(() -> new IllegalArgumentException("No model registered for alias: " + alias));
        return definition.getModelName();
    }

    public ModelHandle defaultModel() {
        if (handlesByAlias.isEmpty()) {
            throw new IllegalStateException("No enabled AI model definition was registered");
        }
        return handlesByAlias.values().iterator().next();
    }

    public String defaultModelName() {
        if (definitionsByAlias.isEmpty()) {
            return "unknown";
        }
        return definitionsByAlias.values().iterator().next().getModelName();
    }

    public List<AiModelDefinition> definitions() {
        return List.copyOf(definitionsByAlias.values());
    }

    private ModelHandle resolve(String alias, List<String> visited) {
        String normalized = normalize(alias);
        ModelHandle handle = handlesByAlias.get(normalized);
        if (handle != null) {
            return handle;
        }
        if (visited.contains(normalized)) {
            throw new IllegalArgumentException("Circular model fallback aliases: " + visited);
        }
        visited.add(normalized);
        AiModelDefinition definition = definitionsByAlias.get(normalized);
        if (definition == null || definition.getFallbackAlias() == null
                || definition.getFallbackAlias().isBlank()) {
            throw new IllegalArgumentException("No model registered for alias: " + alias);
        }
        return resolve(definition.getFallbackAlias(), visited);
    }

    private Optional<AiModelDefinition> findDefinition(String alias) {
        if (alias == null || alias.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalize(alias);
        AiModelDefinition definition = definitionsByAlias.get(normalized);
        if (definition != null) {
            return Optional.of(definition);
        }
        String fallback = definition == null ? null : definition.getFallbackAlias();
        return fallback == null ? Optional.empty() : findDefinition(fallback);
    }

    private void registerAll(Collection<AiModelDefinition> definitions) {
        if (definitions == null) {
            return;
        }
        for (AiModelDefinition definition : definitions) {
            if (definition == null || !isEnabled(definition.getEnabled())) {
                continue;
            }
            String alias = definition.getAlias();
            if (alias != null && !alias.isBlank()) {
                // Keep unavailable definitions so require(alias) can follow fallbackAlias.
                definitionsByAlias.putIfAbsent(normalize(alias), definition);
            }
            try {
                register(definition);
            } catch (RuntimeException exception) {
                log.warn("Skip AI model definition alias={} provider={}: {}",
                        definition.getAlias(), definition.getProvider(), exception.getMessage());
            }
        }
    }

    private Optional<ModelAdapter> findAdapter(String provider) {
        String normalizedProvider = normalize(provider);
        ModelAdapter adapter = adaptersByProvider.get(normalizedProvider);
        if (adapter == null && ("deepseek".equals(normalizedProvider)
                || "open-ai".equals(normalizedProvider))) {
            adapter = adaptersByProvider.get("openai");
        }
        return Optional.ofNullable(adapter);
    }

    private static boolean isEnabled(String enabled) {
        return enabled == null || enabled.isBlank()
                || "1".equals(enabled) || "true".equalsIgnoreCase(enabled)
                || "yes".equalsIgnoreCase(enabled);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
