package com.xqy.sms.ai.application.service.chat;

import com.xqy.sms.ai.infrastructure.assistant.chat.AiChatAssistant;
import com.xqy.sms.ai.infrastructure.model.ModelHandle;
import com.xqy.sms.ai.infrastructure.model.ModelRegistry;
import com.xqy.sms.ai.domain.model.AiConstants;
import com.xqy.sms.ai.application.event.AiStreamEventPublisher;
import com.xqy.sms.ai.infrastructure.cache.RedisChatMemoryStore;
import com.xqy.sms.ai.infrastructure.service.log.AiRequestLogService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.function.Consumer;

@Component
public class AiChatService {

    private final ModelRegistry modelRegistry;
    private final RedisChatMemoryStore chatMemoryStore;
    private final AiRequestLogService requestLogService;
    private final AiStreamEventPublisher eventPublisher;
    private final int chatMemoryMaxMessages;

    public AiChatService(ModelRegistry modelRegistry,
                         RedisChatMemoryStore chatMemoryStore,
                         AiRequestLogService requestLogService,
                         AiStreamEventPublisher eventPublisher,
                         @Value("${sms.ai.chat-memory-max-messages}") int chatMemoryMaxMessages) {
        this.modelRegistry = modelRegistry;
        this.chatMemoryStore = chatMemoryStore;
        this.requestLogService = requestLogService;
        this.eventPublisher = eventPublisher;
        this.chatMemoryMaxMessages = chatMemoryMaxMessages;
    }

    public String sampleChat(String question) {
        return createAssistant(null).sampleChat(question);
    }

    /** Streams a composed answer and reports terminal status to the task-run orchestrator. */
    public void answer(String streamKey, String resultJson, String chatMemoryId,
                       String requestId, String alias, Runnable onSuccess, Consumer<Throwable> onFailure) {
        if (resultJson == null || resultJson.isBlank()) {
            publish(streamKey, AiConstants.STREAM_EVENT.ERROR, "工具没有返回结果");
            return;
        }
        try {
            publish(streamKey, AiConstants.STREAM_EVENT.GENERATING, "生成中");
            createAssistant(alias).answer(chatMemoryId, resultJson)
                    .onPartialResponse(token -> publish(streamKey, AiConstants.STREAM_EVENT.TOKEN, token))
                    .onCompleteResponse(response -> {
                        finishRequest(requestId, response);
                        onSuccess.run();
                        publish(streamKey, AiConstants.STREAM_EVENT.DONE, "完成");
                    })
                    .onError(error -> {
                        requestLogService.fail(requestId, error.getClass().getSimpleName(), error);
                        onFailure.accept(error);
                        publish(streamKey, AiConstants.STREAM_EVENT.ERROR, errorMessage(error));
                    })
                    .start();
        } catch (Exception error) {
            requestLogService.fail(requestId, error.getClass().getSimpleName(), error);
            onFailure.accept(error);
            publish(streamKey, AiConstants.STREAM_EVENT.ERROR, errorMessage(error));
        }
    }

    private void publish(String streamKey, String type, Object data) {
        eventPublisher.publish(streamKey, type, data);
    }

    private String errorMessage(Throwable error) {
        if (error == null || error.getMessage() == null || error.getMessage().isBlank()) {
            return "AI chat failed";
        }
        return error.getMessage();
    }

    private void finishRequest(String requestId, ChatResponse response) {
        if (response == null) {
            requestLogService.success(requestId, null, null, null, null);
            return;
        }
        TokenUsage usage = response.tokenUsage();
        requestLogService.success(requestId, response.modelName(),
                usage == null ? null : usage.inputTokenCount(),
                usage == null ? null : usage.outputTokenCount(),
                usage == null ? null : usage.totalTokenCount());
    }

    private AiChatAssistant createAssistant(String alias) {
        ModelHandle modelHandle = modelRegistry.find(normalizeAlias(alias))
                .orElseGet(modelRegistry::defaultModel);
        return AiServices.builder(AiChatAssistant.class)
                .chatModel(modelHandle.chatModel())
                .streamingChatModel(modelHandle.streamingChatModel())
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(chatMemoryMaxMessages)
                        .chatMemoryStore(chatMemoryStore)
                        .build())
                .build();
    }

    private static String normalizeAlias(String alias) {
        return alias == null || alias.isBlank() ? AiConstants.MODEL_ALIAS.BALANCED : alias.trim();
    }
}
