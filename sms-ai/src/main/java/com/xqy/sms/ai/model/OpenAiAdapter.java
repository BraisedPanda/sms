package com.xqy.sms.ai.model;

import com.xqy.sms.common.entity.AiModelDefinition;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Adapter for OpenAI and OpenAI-compatible providers such as DeepSeek. */
@Component
public class OpenAiAdapter implements ModelAdapter {

    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private final Environment environment;

    public OpenAiAdapter(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String provider() {
        return "openai";
    }

    @Override
    public ModelHandle create(AiModelDefinition modelDefinition) {
        if (modelDefinition == null) {
            throw new IllegalArgumentException("modelDefinition must not be null");
        }
        if (!provider().equals(normalize(modelDefinition.getProvider()))
                && !"deepseek".equals(normalize(modelDefinition.getProvider()))) {
            throw new IllegalArgumentException("Unsupported OpenAI-compatible provider: "
                    + modelDefinition.getProvider());
        }
        String modelName = required(modelDefinition.getModelName(), "modelName");
        String apiKey = resolveApiKey(required(modelDefinition.getApiKey(), "apiKey"));
        String baseUrl = valueOrDefault(modelDefinition.getBaseUrl(), DEFAULT_BASE_URL);
        return new ModelHandle(
                OpenAiChatModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build(),
                OpenAiStreamingChatModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build());
    }

    private String resolveApiKey(String value) {
        String resolved = environment == null ? null : environment.getProperty(value);
        if (resolved == null || resolved.isBlank()) {
            resolved = System.getProperty(value);
        }
        if (resolved == null || resolved.isBlank()) {
            resolved = System.getenv(value);
        }
        if (resolved == null || resolved.isBlank()) {
            // Permit an explicit key in tests or a secret manager-backed database.
            resolved = value.startsWith("sk-") ? value : null;
        }
        return required(resolved, "resolved apiKey");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
