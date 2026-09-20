package com.xqy.sms.ai.infrastructure.event;

import com.xqy.sms.ai.application.event.AiStreamEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/** Delivers AI run events through Redis Streams for independently deployed consumers. */
@Component
public class RedisStreamAiEventPublisher implements AiStreamEventPublisher {
    private final StringRedisTemplate redisTemplate;
    private final Duration eventTtl;

    public RedisStreamAiEventPublisher(StringRedisTemplate redisTemplate,
                                       @Value("${sms.ai.stream-event-ttl-minutes:5}") long eventTtlMinutes) {
        this.redisTemplate = redisTemplate;
        this.eventTtl = Duration.ofMinutes(eventTtlMinutes);
    }

    @Override
    public void publish(String streamKey, String type, Object data) {
        if (streamKey == null || streamKey.isBlank()) {
            throw new IllegalArgumentException("streamKey must not be blank");
        }
        redisTemplate.opsForStream().add(StreamRecords.mapBacked(
                Map.of("event", type, "data", String.valueOf(data))).withStreamKey(streamKey));
        redisTemplate.expire(streamKey, eventTtl);
    }
}
