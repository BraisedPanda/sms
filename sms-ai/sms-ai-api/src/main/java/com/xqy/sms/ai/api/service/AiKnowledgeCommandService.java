package com.xqy.sms.ai.api.service;

import com.xqy.sms.ai.api.model.KnowledgeIngestionCommand;
import com.xqy.sms.ai.api.model.KnowledgeIngestionResult;

public interface AiKnowledgeCommandService {
    void activateIndex(Long documentVersionId, int indexRevision, com.xqy.sms.common.security.rpc.InternalCallContext caller);
    KnowledgeIngestionResult ingest(KnowledgeIngestionCommand command);
}
