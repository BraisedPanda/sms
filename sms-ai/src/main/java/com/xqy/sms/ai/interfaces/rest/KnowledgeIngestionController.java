package com.xqy.sms.ai.interfaces.rest;

import com.xqy.sms.ai.infrastructure.service.knowledge.KnowledgeDocumentIngestionService;
import com.xqy.sms.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP endpoint for importing staged knowledge details into PostgreSQL vectors. */
@RestController
@RequestMapping("/api/ai/knowledge")
public class KnowledgeIngestionController {
    private final KnowledgeDocumentIngestionService ingestionService;

    public KnowledgeIngestionController(KnowledgeDocumentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingestions")
    public ApiResponse<KnowledgeDocumentIngestionService.IngestionResult> ingest(
            @RequestBody(required = false) KnowledgeDocumentIngestionService.IngestionRequest request) {
        return ApiResponse.success(ingestionService.ingest(request));
    }
}
