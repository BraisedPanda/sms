package com.xqy.sms.ai.api.model;

import java.io.Serializable;
import com.xqy.sms.common.security.rpc.InternalCallContext;

/** Transport-neutral command for importing staged knowledge document chunks. */
public record KnowledgeIngestionCommand(Long knowledgeBaseId, Long documentVersionId,
                                        Integer limit, Integer indexRevision, String tenantId,
                                        InternalCallContext callerContext) implements Serializable {
    public KnowledgeIngestionCommand(Long knowledgeBaseId, Long documentVersionId,
                                     Integer limit, Integer indexRevision) {
        this(knowledgeBaseId, documentVersionId, limit, indexRevision, null, null);
    }
}
