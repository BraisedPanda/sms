package com.xqy.sms.ai.service.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Optional OpenAI-compatible embedding client used before calling Milvus. */
@Component
public class KnowledgeEmbeddingService {
    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String endpoint;
    private final String token;
    private final String model;
    private final int dimensions;

    public KnowledgeEmbeddingService(RestClient.Builder builder, ObjectMapper objectMapper,
                                     @Value("${sms.ai.embedding.endpoint:http://localhost:11434/v1/embeddings}") String endpoint,
                                     @Value("${sms.ai.embedding.token:}") String token,
                                     @Value("${sms.ai.embedding.model:bge-m3}") String model,
                                     @Value("${sms.ai.embedding.dimensions:1024}") int dimensions) {
        this.client = builder.build();
        this.objectMapper = objectMapper;
        this.endpoint = endpoint == null ? "" : endpoint.trim();
        this.token = token == null ? "" : token.trim();
        this.model = model;
        if (dimensions <= 0) throw new IllegalArgumentException("Embedding dimensions must be positive");
        this.dimensions = dimensions;
    }

    public List<Float> embed(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        List<List<Float>> embeddings = embedAll(List.of(text));
        return embeddings.isEmpty() ? Collections.emptyList() : embeddings.getFirst();
    }

    /** Creates embeddings in one OpenAI-compatible request and preserves input order. */
    public List<List<Float>> embedAll(List<String> texts) {
        if (endpoint.isBlank() || texts == null || texts.isEmpty()) return Collections.emptyList();
        if (texts.stream().anyMatch(text -> text == null || text.isBlank())) {
            throw new IllegalArgumentException("Embedding input must not be blank");
        }
        try {
            String response = client.post().uri(endpoint).contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> { if (!token.isBlank()) headers.setBearerAuth(token); })
                    .body(Map.of("model", model, "input", texts, "dimensions", dimensions))
                    .retrieve().body(String.class);
            JsonNode data = objectMapper.readTree(response).path("data");
            if (!data.isArray() || data.size() != texts.size()) {
                throw new IllegalStateException("Embedding response count does not match request count");
            }
            List<List<Float>> result = new ArrayList<>(Collections.nCopies(texts.size(), null));
            for (JsonNode item : data) {
                int index = item.path("index").asInt(-1);
                JsonNode embedding = item.path("embedding");
                if (index < 0 || index >= texts.size() || !embedding.isArray() || embedding.size() != dimensions) {
                    throw new IllegalStateException("Embedding response has an invalid index or dimension");
                }
                List<Float> vector = new ArrayList<>(dimensions);
                for (JsonNode value : embedding) vector.add((float) value.asDouble());
                result.set(index, vector);
            }
            if (result.stream().anyMatch(java.util.Objects::isNull)) {
                throw new IllegalStateException("Embedding response is missing an input result");
            }
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create knowledge embedding", exception);
        }
    }
}
