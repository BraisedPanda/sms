package com.xqy.sms.ai.interfaces.dubbo;

import com.xqy.sms.ai.api.model.KnowledgeIngestionCommand;
import com.xqy.sms.ai.api.model.KnowledgeIngestionResult;
import com.xqy.sms.ai.api.service.AiKnowledgeCommandService;
import com.xqy.sms.ai.infrastructure.service.knowledge.KnowledgeIngestionQueue;
import com.xqy.sms.common.security.rpc.InternalCallContext;
import com.xqy.sms.common.security.rpc.InternalCallSigner;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DubboService
public class AiKnowledgeCommandServiceImpl implements AiKnowledgeCommandService {
    private static final Logger log = LoggerFactory.getLogger(AiKnowledgeCommandServiceImpl.class);
    private final KnowledgeIngestionQueue ingestionService;
    private final InternalCallSigner internalCallSigner;

    public AiKnowledgeCommandServiceImpl(KnowledgeIngestionQueue ingestionService,
                                         @Value("${sms.internal-rpc.secret}") String internalRpcSecret) {
        this.ingestionService = ingestionService;
        this.internalCallSigner = new InternalCallSigner(internalRpcSecret);
    }

    @Override
    public KnowledgeIngestionResult ingest(KnowledgeIngestionCommand command) {
        if (command == null) throw new IllegalArgumentException("ingestion command must not be null");
        InternalCallContext context = command.callerContext();
        internalCallSigner.verify(context, "sms-web-bff");
        if (!java.util.Objects.equals(context.tenantId(), command.tenantId())) {
            throw new InternalCallSigner.InternalCallAuthenticationException();
        }
        log.info("rpc_audit action=knowledge_ingest caller={} tenant={} user={} requestId={}", context.callerService(),
                context.tenantId(), context.userId(), context.requestId());
        return ingestionService.enqueue(command);
    }

    @Override public void activateIndex(Long documentVersionId, int indexRevision, InternalCallContext caller) {
        internalCallSigner.verify(caller, "sms-web-bff");
        ingestionService.activateIndex(documentVersionId, indexRevision, caller);
    }
}
