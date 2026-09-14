package com.xqy.sms.knowledge.provider.milvus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xqy.sms.knowledge.api.entity.AiknowledgeChunk;
import com.xqy.sms.knowledge.api.model.KnowledgeVectorQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Small Milvus REST v2 adapter. The SDK is deliberately kept out of the API module. */
@Component
@ConditionalOnProperty(name = "sms.knowledge.vector-store", havingValue = "milvus", matchIfMissing = true)
public class MilvusRestVectorStore implements MilvusVectorStore {
    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String endpoint;
    private final String token;
    private final String defaultCollection;

    public MilvusRestVectorStore(RestClient.Builder builder, ObjectMapper objectMapper,
                                 @Value("${sms.knowledge.milvus.endpoint:}") String endpoint,
                                 @Value("${sms.knowledge.milvus.token:}") String token,
                                 @Value("${sms.knowledge.milvus.collection:ai_knowledge_chunk}") String defaultCollection) {
        this.client = builder.build();
        this.objectMapper = objectMapper;
        this.endpoint = normalizeEndpoint(endpoint);
        this.token = token == null ? "" : token.trim();
        this.defaultCollection = defaultCollection;
    }

    @Override
    public List<AiknowledgeChunk> search(KnowledgeVectorQuery query) {
        if (query == null || query.getEmbedding() == null || query.getEmbedding().isEmpty() || endpoint.isBlank()) {
            return Collections.emptyList();
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("collectionName", query.getCollectionName() == null || query.getCollectionName().isBlank()
                ? defaultCollection : query.getCollectionName());
        ArrayNode data = body.putArray("data");
        data.add(objectMapper.valueToTree(query.getEmbedding()));
        body.put("limit", safeLimit(query.getTopK()));
        body.putArray("outputFields").add("*");
        String filter = buildFilter(query);
        if (!filter.isBlank()) {
            body.put("filter", filter);
        }
        String json = client.post().uri(endpoint).contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> { if (!token.isBlank()) headers.setBearerAuth(token); })
                .body(body).retrieve().body(String.class);
        return parse(json);
    }

    private List<AiknowledgeChunk> parse(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.has("data") ? root.get("data") : root;
            if (!data.isArray()) return Collections.emptyList();
            List<AiknowledgeChunk> result = new ArrayList<>();
            for (JsonNode item : data) {
                JsonNode fields = item.has("entity") && item.get("entity").isObject() ? item.get("entity") : item;
                AiknowledgeChunk chunk = new AiknowledgeChunk();
                chunk.setKnowledgeBaseId(longValue(fields, "knowledgeBaseId", "knowledge_base_id"));
                chunk.setDocumentId(longValue(fields, "documentId", "document_id"));
                chunk.setDocumentNo(textValue(fields, "documentNo", "document_no"));
                chunk.setDocumentVersionId(longValue(fields, "documentVersionId", "document_version_id"));
                chunk.setIndexRevision(textValue(fields, "indexRevision", "index_revision"));
                chunk.setChunkNo(intValue(fields, "chunkNo", "chunk_no"));
                chunk.setContent(textValue(fields, "content"));
                chunk.setStatus(textValue(fields, "status"));
                if (item.has("distance")) chunk.setScore(item.get("distance").asDouble());
                if (item.has("score")) chunk.setScore(item.get("score").asDouble());
                JsonNode metadata = fields.get("metadata");
                if (metadata != null && metadata.isObject()) {
                    chunk.setMetadata(objectMapper.convertValue(metadata, Map.class));
                }
                result.add(chunk);
            }
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse Milvus search response", exception);
        }
    }

    private String buildFilter(KnowledgeVectorQuery query) {
        List<String> expressions = new ArrayList<>();
        if (query.getKnowledgeBaseId() != null) expressions.add("knowledgeBaseId == " + query.getKnowledgeBaseId());
        expressions.add("status == 'ACTIVE'");
        Map<String, Object> filter = query.getFilter();
        if (filter != null) {
            filter.forEach((key, value) -> expressions.add(key + " == " + quote(value)));
        }
        return String.join(" and ", expressions);
    }
    private String quote(Object value) {
        if (value instanceof Number || value instanceof Boolean) return String.valueOf(value);
        return "'" + String.valueOf(value).replace("'", "\\'") + "'";
    }
    private int safeLimit(Integer value) { return value == null || value <= 0 ? 10 : Math.min(value, 1000); }
    private String normalizeEndpoint(String value) {
        if (value == null || value.isBlank()) return "";
        String normalized = value.trim();
        if (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        if (normalized.matches("https?://[^/]+")) {
            return normalized + "/v2/vectordb/entities/search";
        }
        return normalized;
    }
    private String textValue(JsonNode node, String... names) { for (String name : names) if (node.hasNonNull(name)) return node.get(name).asText(); return null; }
    private Long longValue(JsonNode node, String... names) { String value = textValue(node, names); try { return value == null ? null : Long.valueOf(value); } catch (NumberFormatException ignored) { return null; } }
    private Integer intValue(JsonNode node, String... names) { String value = textValue(node, names); try { return value == null ? null : Integer.valueOf(value); } catch (NumberFormatException ignored) { return null; } }
}
