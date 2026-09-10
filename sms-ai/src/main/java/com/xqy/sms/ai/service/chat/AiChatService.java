package com.xqy.sms.ai.service.chat;

import com.xqy.sms.ai.service.chat.assistant.AiChatAssistant;
import com.xqy.sms.ai.model.ModelHandle;
import com.xqy.sms.ai.model.ModelRegistry;
import com.xqy.sms.ai.store.RedisChatMemoryStore;
import com.xqy.sms.ai.service.log.AiRequestLogService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.stereotype.Component;

@Component
public class AiChatService {

    private final ModelRegistry modelRegistry;
    private final RedisChatMemoryStore chatMemoryStore;
    private final AiRequestLogService requestLogService;

    public AiChatService(ModelRegistry modelRegistry,
                         RedisChatMemoryStore chatMemoryStore,
                         AiRequestLogService requestLogService) {
        this.modelRegistry = modelRegistry;
        this.chatMemoryStore = chatMemoryStore;
        this.requestLogService = requestLogService;
    }

    public String sampleChat(String question) {
        return createAssistant(null).sampleChat(question);
    }

    public void streamChat(SseEmitter emitter, String question, String chatMemoryId, String requestId) {
        streamChat(emitter, question, chatMemoryId, requestId, null);
    }

    public void streamChat(SseEmitter emitter, String question, String chatMemoryId,
                           String requestId, String alias) {
        if (emitter == null) {
            throw new IllegalArgumentException("emitter must not be null");
        }
        if (question == null || question.isBlank()) {
            sendEvent(emitter, "error", "question must not be blank");
            emitter.complete();
            return;
        }
        if (chatMemoryId == null || chatMemoryId.isBlank()) {
            sendEvent(emitter, "error", "chatMemoryId must not be blank");
            emitter.complete();
            return;
        }
        question = "用户问题：" + question + "。输出的格式为markdown，且不需要额外的解释说明。";
        try {
            sendEvent(emitter, "generating", "生成中");
            createAssistant(alias).chat(chatMemoryId, question)
                    .onPartialResponse(token -> sendEvent(emitter, "token", token))
                    .onCompleteResponse(response -> {
                        finishRequest(requestId, response);
                        sendEvent(emitter, "done", "完成");
                        emitter.complete();
                    })
                    .onError(error -> {
                        requestLogService.fail(requestId, error.getClass().getSimpleName(), error);
                        sendEvent(emitter, "error", errorMessage(error));
                        emitter.completeWithError(error);
                    })
                    .start();
        } catch (Exception error) {
            requestLogService.fail(requestId, error.getClass().getSimpleName(), error);
            sendEvent(emitter, "error", errorMessage(error));
            emitter.completeWithError(error);
        }
    }

    /** Streams a concise answer based on structured business-tool results. */
    public void answer(SseEmitter emitter, String resultJson, String chatMemoryId) {
        answer(emitter, resultJson, chatMemoryId, null);
    }

    public void answer(SseEmitter emitter, String resultJson, String chatMemoryId, String requestId) {
        answer(emitter, resultJson, chatMemoryId, requestId, null);
    }

    public void answer(SseEmitter emitter, String resultJson, String chatMemoryId,
                       String requestId, String alias) {
        if (resultJson == null || resultJson.isBlank()) {
            sendEvent(emitter, "error", "工具没有返回结果");
            emitter.complete();
            return;
        }
        try {
            sendEvent(emitter, "generating", "生成中");
            createAssistant(alias).answer(chatMemoryId, resultJson)
                    .onPartialResponse(token -> sendEvent(emitter, "token", token))
                    .onCompleteResponse(response -> {
                        finishRequest(requestId, response);
                        sendEvent(emitter, "done", "完成");
                        emitter.complete();
                    })
                    .onError(error -> {
                        requestLogService.fail(requestId, error.getClass().getSimpleName(), error);
                        sendEvent(emitter, "error", errorMessage(error));
                        emitter.completeWithError(error);
                    })
                    .start();
        } catch (Exception error) {
            requestLogService.fail(requestId, error.getClass().getSimpleName(), error);
            sendEvent(emitter, "error", errorMessage(error));
            emitter.completeWithError(error);
        }
    }

    private void sendEvent(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (Exception sendError) {
            emitter.completeWithError(sendError);
        }
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
                        .maxMessages(40)
                        .chatMemoryStore(chatMemoryStore)
                        .build())
                .build();
    }

    private static String normalizeAlias(String alias) {
        return alias == null || alias.isBlank() ? "balanced" : alias.trim();
    }
}
