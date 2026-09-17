package com.xqy.sms.ai.api.service;

import com.xqy.sms.ai.api.model.KnowledgeIngestionCommand;
import com.xqy.sms.ai.api.model.KnowledgeIngestionResult;

public interface AiKnowledgeCommandService {
    KnowledgeIngestionResult ingest(KnowledgeIngestionCommand command);
}
