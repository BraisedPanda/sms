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
    private final java.util.concurrent.ConcurrentMap<String, StreamState> streams = new java.util.concurrent.ConcurrentHashMap<>();
    private static class StreamState {
        final java.util.concurrent.atomic.AtomicBoolean terminal = new java.util.concurrent.atomic.AtomicBoolean();
        volatile dev.langchain4j.model.chat.response.StreamingHandle handle;
        volatile boolean cancelled;
        Runnable onCancel;
        long lastCancelCheck;
    }

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
                       String requestId, String alias, Consumer<String> onSuccess, Consumer<Throwable> onFailure) {
        answer(streamKey,resultJson,chatMemoryId,requestId,alias,onSuccess,onFailure,()->false,java.util.List.of());
    }

    public void cancel(String streamKey) {
        if (streamKey == null) return;
        StreamState state = streams.get(streamKey);
        if (state == null) return;
        state.cancelled = true;
        if (state.handle != null) state.handle.cancel();
        if (state.onCancel != null) state.onCancel.run();
    }

    public void answer(String streamKey, String resultJson, String chatMemoryId,
                       String requestId, String alias, Consumer<String> onSuccess, Consumer<Throwable> onFailure,
                       java.util.function.BooleanSupplier cancellationRequested,
                       java.util.List<dev.langchain4j.data.message.ChatMessage> restoredMemory) {
        StreamState state = new StreamState();
        state.onCancel = () -> {
            if (state.terminal.compareAndSet(false,true)) {
                try { fail(streamKey,requestId,onFailure,new com.xqy.sms.ai.application.service.execution.TaskExecutionService.TaskCancelledException()); }
                finally { streams.remove(streamKey,state); }
            }
        };
        streams.put(streamKey,state);
        if (resultJson == null || resultJson.isBlank()) {
            streams.remove(streamKey,state);
            fail(streamKey, requestId, onFailure, new IllegalArgumentException("工具没有返回结果"));
            return;
        }
        try {
            if (cancellationRequested.getAsBoolean()) { cancel(streamKey); return; }
            chatMemoryStore.initializeIfAbsent(chatMemoryId, restoredMemory);
            publish(streamKey, AiConstants.STREAM_EVENT.GENERATING, "生成中");
            createAssistant(alias).answer(chatMemoryId, resultJson)
                    .onPartialResponseWithContext((response, context) -> {
                        state.handle = context.streamingHandle();
                        long now = System.currentTimeMillis();
                        if (now-state.lastCancelCheck >= 100) {
                            state.lastCancelCheck = now;
                            if (cancellationRequested.getAsBoolean()) state.cancelled = true;
                        }
                        if (state.cancelled) { state.handle.cancel(); state.onCancel.run(); return; }
                        if (!state.terminal.get()) publish(streamKey, AiConstants.STREAM_EVENT.TOKEN, response.text());
                    })
                    .onCompleteResponse(response -> {
                        if (!state.terminal.compareAndSet(false,true)) return;
                        try {
                            onSuccess.accept(response == null || response.aiMessage() == null ? null : response.aiMessage().text());
                            finishRequest(requestId, response);
                            publish(streamKey, AiConstants.STREAM_EVENT.DONE, "完成");
                        } catch (Exception error) {
                            fail(streamKey, requestId, onFailure, error);
                        } finally {
                            streams.remove(streamKey,state);
                        }
                    })
                    .onError(error -> {
                        if (!state.terminal.compareAndSet(false,true)) return;
                        try { fail(streamKey, requestId, onFailure, error); }
                        finally { streams.remove(streamKey,state); }
                    })
                    .start();
        } catch (Exception error) {
            if (state.terminal.compareAndSet(false,true)) {
                try { fail(streamKey, requestId, onFailure, error); }
                finally { streams.remove(streamKey,state); }
            }
        }
    }

    private void fail(String streamKey, String requestId, Consumer<Throwable> onFailure, Throwable error) {
        requestLogService.fail(requestId, error.getClass().getSimpleName(), error);
        try {
            onFailure.accept(error);
        } finally {
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
