package com.xqy.sms.web.interfaces.rest;

import com.xqy.sms.ai.api.model.AiChatCommand;
import com.xqy.sms.ai.api.service.AiChatCommandService;
import com.xqy.sms.common.dto.ApiResponse;
import com.xqy.sms.web.application.auth.AuthService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** HTTP edge for AI streaming. The provider emits every SSE event into the Redis Stream. */
@RestController
@RequestMapping("/api/ai")
public class AiChatController {
    @DubboReference(check = false) private AiChatCommandService chatService;
    private final StringRedisTemplate redis;
    public AiChatController(StringRedisTemplate redis) { this.redis = redis; }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatRequest request, Authentication authentication) {
        if (request == null || request.question() == null || request.question().isBlank()) throw new IllegalArgumentException("question must not be blank");
        if (!(authentication != null && authentication.getPrincipal() instanceof AuthService.AuthenticatedUser user)) throw new IllegalArgumentException("unauthenticated request");
        String key = "sms:ai:sse:" + UUID.randomUUID();
        chatService.submit(new AiChatCommand(user.userId(), user.sessionId(), request.question(), request.alias(), request.idempotencyKey(), key));
        SseEmitter emitter = new SseEmitter(120_000L);
        Thread.ofVirtual().start(() -> relay(key, emitter));
        return emitter;
    }

    @PostMapping("/runs/{runId}/cancel")
    public ApiResponse<Void> cancel(@PathVariable String runId, Authentication authentication) {
        if (!(authentication != null && authentication.getPrincipal() instanceof AuthService.AuthenticatedUser user)) throw new IllegalArgumentException("unauthenticated request");
        chatService.cancel(runId, user.userId());
        return ApiResponse.success(null);
    }

    @SuppressWarnings("unchecked")
    private void relay(String key, SseEmitter emitter) {
        String lastId = "0-0";
        try {
            while (true) {
                List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
                        StreamReadOptions.empty().block(Duration.ofSeconds(2)).count(100),
                        StreamOffset.create(key, ReadOffset.from(lastId)));
                if (records != null) for (MapRecord<String, Object, Object> record : records) {
                    lastId = record.getId().getValue(); Map<Object, Object> body = record.getValue(); String event = String.valueOf(body.get("event"));
                    emitter.send(SseEmitter.event().name(event).data(body.get("data"))); if ("done".equals(event) || "error".equals(event)) { emitter.complete(); return; }
                }
            }
        } catch (Exception error) { emitter.completeWithError(error); }
        finally { redis.delete(key); }
    }
    public record ChatRequest(String question, String alias, String idempotencyKey) { }
}
