package com.xqy.sms.ai.service.transport;

import com.xqy.sms.ai.model.AiConstants;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Keeps HTTP/SSE transport details out of task planning and execution services. */
@Service
public class SseTransportService {
    public SseEmitter createEmitter() {
        return new SseEmitter(AiConstants.SSE_EMITTER_TIMEOUT_MILLIS);
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
