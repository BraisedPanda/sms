package com.xqy.sms.ai.store;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/** Stores LangChain4j chat memory as JSON values in Redis. */
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final String DEFAULT_KEY_PREFIX = "chat-memory:";

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;

    public RedisChatMemoryStore(StringRedisTemplate redisTemplate) {
        this(redisTemplate, DEFAULT_KEY_PREFIX);
    }

    public RedisChatMemoryStore(StringRedisTemplate redisTemplate, String keyPrefix) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        this.keyPrefix = Objects.requireNonNull(keyPrefix, "keyPrefix must not be null");
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String messagesJson = redisTemplate.opsForValue().get(key(memoryId));
        if (messagesJson == null) {
            return List.of();
        }
        return ChatMessageDeserializer.messagesFromJson(messagesJson);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        Objects.requireNonNull(messages, "messages must not be null");
        String messagesJson = ChatMessageSerializer.messagesToJson(messages);
        redisTemplate.opsForValue().set(key(memoryId), messagesJson);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redisTemplate.delete(key(memoryId));
    }

    private String key(Object memoryId) {
        return keyPrefix + Objects.requireNonNull(memoryId, "memoryId must not be null");
    }
}
