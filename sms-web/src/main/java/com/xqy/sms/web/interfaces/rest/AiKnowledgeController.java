package com.xqy.sms.web.interfaces.rest;

import com.xqy.sms.ai.api.model.KnowledgeIngestionCommand;
import com.xqy.sms.ai.api.model.KnowledgeIngestionResult;
import com.xqy.sms.ai.api.service.AiKnowledgeCommandService;
import com.xqy.sms.common.dto.ApiResponse;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated Web BFF edge for knowledge indexing commands. */
@RestController
@RequestMapping("/api/ai/knowledge")
public class AiKnowledgeController {
    @DubboReference(check = false)
    private AiKnowledgeCommandService knowledgeService;

    @PostMapping("/ingestions")
    @PreAuthorize("hasAnyRole('R_SUPER', 'R_ADMIN')")
    public ApiResponse<KnowledgeIngestionResult> ingest(@RequestBody(required = false) KnowledgeIngestionCommand command) {
        return ApiResponse.success(knowledgeService.ingest(command));
    }
}
