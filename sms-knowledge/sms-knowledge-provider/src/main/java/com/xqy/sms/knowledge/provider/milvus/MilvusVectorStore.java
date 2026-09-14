package com.xqy.sms.knowledge.provider.milvus;

import com.xqy.sms.knowledge.api.entity.AiknowledgeChunk;
import com.xqy.sms.knowledge.api.model.KnowledgeVectorQuery;
import java.util.List;

public interface MilvusVectorStore {
    List<AiknowledgeChunk> search(KnowledgeVectorQuery query);
}
