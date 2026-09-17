package com.xqy.sms.ai.api.model;

import java.io.Serializable;

/** Transport-neutral command sent from the Web BFF to the AI provider. */
public record AiChatCommand(Long userId, Long sessionId, String question, String alias,
                            String idempotencyKey, String streamKey) implements Serializable { }
