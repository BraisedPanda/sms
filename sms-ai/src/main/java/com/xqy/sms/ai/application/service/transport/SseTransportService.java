package com.xqy.sms.ai.application.service.transport;

import com.xqy.sms.ai.domain.model.AiConstants;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Keeps HTTP/SSE transport details out of task planning and execution services. */
@Service
public class SseTransportService {
    private final long emitterTimeoutMillis;

    public SseTransportService(@Value("${sms.ai.sse-emitter-timeout-millis}") long emitterTimeoutMillis) {
        this.emitterTimeoutMillis = emitterTimeoutMillis;
    }

    public SseEmitter createEmitter() {
        return new SseEmitter(emitterTimeoutMillis);
    }

    public void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (Exception error) {
            emitter.completeWithError(error);
        }
    }

    public void error(SseEmitter emitter, Throwable error) {
        send(emitter, AiConstants.SSE_EVENT.ERROR,
                error == null || error.getMessage() == null ? "AI task failed" : error.getMessage());
        emitter.completeWithError(error == null ? new IllegalStateException("AI task failed") : error);
    }
}
