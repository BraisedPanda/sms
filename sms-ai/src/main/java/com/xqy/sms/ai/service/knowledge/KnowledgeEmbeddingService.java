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

/** Optional OpenAI-compatible embedding client used before calling Milvus. */
@Component
public class KnowledgeEmbeddingService {
    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String endpoint;
    private final String token;
    private final String model;

    public KnowledgeEmbeddingService(RestClient.Builder builder, ObjectMapper objectMapper,
                                     @Value("${sms.ai.embedding.endpoint:}") String endpoint,
                                     @Value("${sms.ai.embedding.token:}") String token,
                                     @Value("${sms.ai.embedding.model:text-embedding-3-small}") String model) {
        this.client = builder.build();
        this.objectMapper = objectMapper;
        this.endpoint = endpoint == null ? "" : endpoint.trim();
        this.token = token == null ? "" : token.trim();
        this.model = model;
    }

    public List<Float> embed(String text) {
        if (endpoint.isBlank() || text == null || text.isBlank()) return Collections.emptyList();
        try {
            String response = client.post().uri(endpoint).contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> { if (!token.isBlank()) headers.setBearerAuth(token); })
                    .body(java.util.Map.of("model", model, "input", text)).retrieve().body(String.class);
            JsonNode data = objectMapper.readTree(response).path("data").path(0).path("embedding");
            if (!data.isArray()) return Collections.emptyList();
            List<Float> result = new ArrayList<>();
            for (JsonNode value : data) result.add((float) value.asDouble());
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create knowledge query embedding", exception);
        }
    }
}
