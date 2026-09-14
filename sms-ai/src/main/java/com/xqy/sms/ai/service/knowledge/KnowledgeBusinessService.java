package com.xqy.sms.ai.service.knowledge;

import com.xqy.sms.ai.model.AiTask;
import com.xqy.sms.ai.model.AiTaskResult;
import com.xqy.sms.ai.model.QueryCriteria;
import com.xqy.sms.ai.model.QueryFilterNode;
import com.xqy.sms.ai.model.ToolSource;
import com.xqy.sms.knowledge.api.entity.AiknowledgeChunk;
import com.xqy.sms.knowledge.api.model.KnowledgeVectorQuery;
import com.xqy.sms.knowledge.api.service.KnowledgeService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** AI-facing adapter that translates an AiTask into a Milvus vector query. */
@Service
public class KnowledgeBusinessService {
    @DubboReference(check = false)
    private KnowledgeService knowledgeService;
    @Autowired(required = false)
    private KnowledgeEmbeddingService embeddingService;

    public AiTaskResult queryKnowledge(AiTask task) {
        KnowledgeVectorQuery request = toVectorQuery(task);
        List<AiknowledgeChunk> chunks = knowledgeService.queryVector(request);
        AiTaskResult result = new AiTaskResult();
        result.setDomain(task == null ? "knowledge" : task.getDomain());
        result.setToolName(task == null ? "query_knowledge" : task.getToolName());
        result.setQuestion(task == null ? request.getQueryText() : task.getSubQuestion());
        result.setSuccess(true);
        result.setQuery(task == null ? null : task.getQuery());
        result.setItems(chunks);
        result.setSources(toSources(chunks));
        Map<String, Object> summary = new HashMap<>();
        summary.put("domain", "knowledge");
        summary.put("toolName", "query_knowledge");
        summary.put("count", chunks == null ? 0 : chunks.size());
        summary.put("topK", request.getTopK());
        result.setSummary(summary);
        return result;
    }

    public AiTaskResult searchKnowledge(AiTask task) {
        return queryKnowledge(task);
    }

    /** Exposed for tests and other AI flows that need the normalized provider request. */
    public KnowledgeVectorQuery toVectorQuery(AiTask task) {
        KnowledgeVectorQuery request = new KnowledgeVectorQuery();
        QueryCriteria criteria = task == null ? null : task.getQuery();
        if (criteria != null) {
            request.setKnowledgeBaseId(criteria.getKnowledgeBaseId());
            request.setQueryText(criteria.getQueryText());
            request.setEmbedding(criteria.getEmbedding());
            request.setTopK(criteria.getTopK() != null
                    ? criteria.getTopK()
                    : (criteria.getLimit() > 0 ? criteria.getLimit() : null));
            request.setSimilarityThreshold(criteria.getSimilarityThreshold());
            request.setFilter(extractFilter(criteria.getFilter()));
        }
        if (request.getQueryText() == null || request.getQueryText().isBlank()) {
            request.setQueryText(task == null ? null : firstNonBlank(task.getSubQuestion(), task.getReason()));
        }
        if ((request.getEmbedding() == null || request.getEmbedding().isEmpty()) && task != null) {
            request.setEmbedding(task.getEmbedding());
        }
        if ((request.getEmbedding() == null || request.getEmbedding().isEmpty())
                && embeddingService != null && request.getQueryText() != null) {
            request.setEmbedding(embeddingService.embed(request.getQueryText()));
        }
        if (request.getTopK() != null && request.getTopK() <= 0) request.setTopK(null);
        return request;
    }

    private List<ToolSource> toSources(List<AiknowledgeChunk> chunks) {
        List<ToolSource> sources = new ArrayList<>();
        if (chunks == null) return sources;
        int rank = 1;
        for (AiknowledgeChunk chunk : chunks) {
            ToolSource source = new ToolSource(chunk.getDocumentNo(), chunk.getChunkNo() == null ? null : String.valueOf(chunk.getChunkNo()));
            source.setContent(chunk.getContent());
            source.setScore(chunk.getScore());
            source.setRank(rank++);
            source.setMetadata(chunk.getMetadata());
            sources.add(source);
        }
        return sources;
    }

    private Map<String, Object> extractFilter(QueryFilterNode node) {
        Map<String, Object> values = new HashMap<>();
        collectEqualityFilters(node, values);
        return values;
    }
    private void collectEqualityFilters(QueryFilterNode node, Map<String, Object> values) {
        if (node == null) return;
        if (node.getField() != null && node.getOperator() != null
                && "EQ".equals(node.getOperator().toUpperCase(Locale.ROOT)) && node.getValue() != null) {
            values.put(node.getField(), node.getValue());
        }
        if (node.getAnd() != null) node.getAnd().forEach(child -> collectEqualityFilters(child, values));
    }
    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
