package com.xqy.sms.ai.interfaces.dubbo;

import com.xqy.sms.ai.api.model.KnowledgeIngestionCommand;
import com.xqy.sms.ai.api.model.KnowledgeIngestionResult;
import com.xqy.sms.ai.api.service.AiKnowledgeCommandService;
import com.xqy.sms.ai.infrastructure.service.knowledge.KnowledgeDocumentIngestionService;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class AiKnowledgeCommandServiceImpl implements AiKnowledgeCommandService {
    private final KnowledgeDocumentIngestionService ingestionService;

    public AiKnowledgeCommandServiceImpl(KnowledgeDocumentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Override
    public KnowledgeIngestionResult ingest(KnowledgeIngestionCommand command) {
        KnowledgeDocumentIngestionService.IngestionRequest request = command == null ? null
                : new KnowledgeDocumentIngestionService.IngestionRequest(command.knowledgeBaseId(),
                command.documentVersionId(), command.limit(), command.indexRevision());
        KnowledgeDocumentIngestionService.IngestionResult result = ingestionService.ingest(request);
        return new KnowledgeIngestionResult(result.selected(), result.imported(), result.indexRevision());
    }
}
