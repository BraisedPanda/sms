package com.xqy.sms.ai.service.chat;

import com.xqy.sms.ai.service.chat.assistant.AiChatAssistant;
import com.xqy.sms.ai.store.RedisChatMemoryStore;
import com.xqy.sms.ai.service.log.AiRequestLogService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.stereotype.Component;

@Component
public class AiChatService {

    private final AiChatAssistant aiChatAssistant;
    private final AiRequestLogService requestLogService;

    public AiChatService(OpenAiChatModel openAiChatModel,
                         StreamingChatModel streamingChatModel,
                         RedisChatMemoryStore chatMemoryStore,
                         AiRequestLogService requestLogService) {
        this.requestLogService = requestLogService;
        this.aiChatAssistant = AiServices.builder(AiChatAssistant.class)
                .chatModel(openAiChatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(40)
                        .chatMemoryStore(chatMemoryStore)
                        .build())
                .build();
    }

    public String sampleChat(String question) {
        return aiChatAssistant.sampleChat(question);
    }

    /** Streams model output to the client and persists the conversation in Redis. */
    public void streamChat(SseEmitter emitter, String question, String chatMemoryId) {
        streamChat(emitter, question, chatMemoryId, null);
    }

    public void streamChat(SseEmitter emitter, String question, String chatMemoryId, String requestId) {
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

        try {
            aiChatAssistant.chat(chatMemoryId, question)
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
        if (resultJson == null || resultJson.isBlank()) {
            sendEvent(emitter, "error", "工具没有返回结果");
            emitter.complete();
            return;
        }
        try {
            aiChatAssistant.answer(chatMemoryId, resultJson)
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
}
