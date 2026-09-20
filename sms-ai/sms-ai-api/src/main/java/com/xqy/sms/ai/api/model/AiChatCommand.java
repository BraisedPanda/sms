package com.xqy.sms.ai.api.model;

import java.io.Serializable;
import com.xqy.sms.common.security.rpc.InternalCallContext;

/** Transport-neutral command sent from the Web BFF to the AI provider. */
public record AiChatCommand(Long userId, Long sessionId, String question, String alias,
                            String idempotencyKey, String streamKey, String tenantId,
                            InternalCallContext callerContext) implements Serializable {
    public AiChatCommand(Long userId, Long sessionId, String question, String alias,
                         String idempotencyKey, String streamKey) {
        this(userId, sessionId, question, alias, idempotencyKey, streamKey, null, null);
    }
}
