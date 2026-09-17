package com.xqy.sms.ai.application.service.transport;

import com.xqy.sms.ai.domain.model.AiConstants;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.stream.StreamRecords;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Duration;

/** Keeps HTTP/SSE transport details out of task planning and execution services. */
@Service
public class SseTransportService {
    private final long emitterTimeoutMillis;
    private final StringRedisTemplate redisTemplate;
    private final Map<SseEmitter, String> streamKeys = new ConcurrentHashMap<>();

    public SseTransportService(@Value("${sms.ai.sse-emitter-timeout-millis}") long emitterTimeoutMillis,
                               StringRedisTemplate redisTemplate) {
        this.emitterTimeoutMillis = emitterTimeoutMillis;
        this.redisTemplate = redisTemplate;
    }

    public SseEmitter createEmitter() {
        return new SseEmitter(emitterTimeoutMillis);
    }

    public SseEmitter createStreamEmitter(String streamKey) {
        SseEmitter emitter = createEmitter();
        streamKeys.put(emitter, streamKey);
        emitter.onCompletion(() -> streamKeys.remove(emitter));
        emitter.onTimeout(() -> streamKeys.remove(emitter));
        return emitter;
    }

    public void send(SseEmitter emitter, String event, Object data) {
        try {
            String streamKey = streamKeys.get(emitter);
            if (streamKey != null) {
                redisTemplate.opsForStream().add(StreamRecords.mapBacked(Map.of("event", event, "data", String.valueOf(data))).withStreamKey(streamKey));
                redisTemplate.expire(streamKey, Duration.ofMinutes(5));
                if (AiConstants.SSE_EVENT.DONE.equals(event) || AiConstants.SSE_EVENT.ERROR.equals(event)) streamKeys.remove(emitter);
                return;
            }
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
