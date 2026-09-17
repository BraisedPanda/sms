package com.xqy.sms.ai.infrastructure.model;

import com.xqy.sms.common.entity.AiModelDefinition;
import com.xqy.sms.ai.domain.model.AiConstants;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;

import java.util.Locale;

/** Adapter for OpenAI and OpenAI-compatible providers such as DeepSeek. */
@Component
public class OpenAiAdapter implements ModelAdapter {

    private final Environment environment;

    public OpenAiAdapter(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String provider() {
        return AiConstants.MODEL_PROVIDER.OPENAI;
    }

    @Override
    public ModelHandle create(AiModelDefinition modelDefinition) {
        if (modelDefinition == null) {
            throw new IllegalArgumentException("modelDefinition must not be null");
        }
        if (!provider().equals(normalize(modelDefinition.getProvider()))
                && !AiConstants.MODEL_PROVIDER.DEEPSEEK.equals(normalize(modelDefinition.getProvider()))) {
            throw new IllegalArgumentException("Unsupported OpenAI-compatible provider: "
                    + modelDefinition.getProvider());
        }
        String modelName = required(modelDefinition.getModelName(), "modelName");
        String apiKey = resolveApiKey(required(modelDefinition.getApiKey(), "apiKey"));
        String baseUrl = required(modelDefinition.getBaseUrl(), "baseUrl");
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
        String resolved = environment.getProperty(value);
        if (resolved == null || resolved.isBlank()) resolved = System.getenv(value);
        return required(resolved, "resolved apiKey");
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
