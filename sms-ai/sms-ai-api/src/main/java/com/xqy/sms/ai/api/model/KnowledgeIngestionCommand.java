package com.xqy.sms.ai.api.model;

import java.io.Serializable;
import com.xqy.sms.common.security.rpc.InternalCallContext;

public class KnowledgeIngestionCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long knowledgeBaseId;
    private Long documentVersionId;
    private Integer limit;
    private Integer indexRevision;
    private String tenantId;
    private InternalCallContext callerContext;

    public KnowledgeIngestionCommand() { }

    public KnowledgeIngestionCommand(Long knowledgeBaseId, Long documentVersionId, Integer limit, Integer indexRevision) {
        this(knowledgeBaseId, documentVersionId, limit, indexRevision, null, null);
    }

    public KnowledgeIngestionCommand(Long knowledgeBaseId, Long documentVersionId, Integer limit, Integer indexRevision, String tenantId, InternalCallContext callerContext) {
        this.knowledgeBaseId = knowledgeBaseId;
        this.documentVersionId = documentVersionId;
        this.limit = limit;
        this.indexRevision = indexRevision;
        this.tenantId = tenantId;
        this.callerContext = callerContext;
    }

    public Long getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(Long knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
    public Long getDocumentVersionId() { return documentVersionId; }
    public void setDocumentVersionId(Long documentVersionId) { this.documentVersionId = documentVersionId; }
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
    public Integer getIndexRevision() { return indexRevision; }
    public void setIndexRevision(Integer indexRevision) { this.indexRevision = indexRevision; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public InternalCallContext getCallerContext() { return callerContext; }
    public void setCallerContext(InternalCallContext callerContext) { this.callerContext = callerContext; }

    /** Compatibility accessors retained for existing RPC callers. */
    public Long knowledgeBaseId() { return knowledgeBaseId; }
    public Long documentVersionId() { return documentVersionId; }
    public Integer limit() { return limit; }
    public Integer indexRevision() { return indexRevision; }
    public String tenantId() { return tenantId; }
    public InternalCallContext callerContext() { return callerContext; }
}
