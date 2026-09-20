package com.xqy.sms.web.interfaces.rest;

import com.xqy.sms.ai.api.model.KnowledgeIngestionCommand;
import com.xqy.sms.ai.api.model.KnowledgeIngestionResult;
import com.xqy.sms.ai.api.service.AiKnowledgeCommandService;
import com.xqy.sms.common.dto.ApiResponse;
import com.xqy.sms.common.security.rpc.InternalCallSigner;
import com.xqy.sms.web.application.auth.AuthService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import java.util.UUID;

/** Authenticated Web BFF edge for knowledge indexing commands. */
@RestController
@RequestMapping("/api/ai/knowledge")
public class AiKnowledgeController {
    @DubboReference(check = false)
    private AiKnowledgeCommandService knowledgeService;
    private final InternalCallSigner internalCallSigner;

    public AiKnowledgeController(@Value("${sms.internal-rpc.secret}") String internalRpcSecret) {
        this.internalCallSigner = new InternalCallSigner(internalRpcSecret);
    }

    @PostMapping("/ingestions")
    @PreAuthorize("hasAnyRole('R_SUPER', 'R_ADMIN')")
    public ApiResponse<KnowledgeIngestionResult> ingest(@RequestBody(required = false) KnowledgeIngestionCommand command,
                                                          Authentication authentication) {
        if (!(authentication != null && authentication.getPrincipal() instanceof AuthService.AuthenticatedUser user)) {
            throw new IllegalArgumentException("unauthenticated request");
        }
        if (user.tenantId() == null || user.tenantId().isBlank()) throw new IllegalArgumentException("tenant is required");
        if (command != null && command.limit() != null && (command.limit() <= 0 || command.limit() > 10_000)) {
            throw new IllegalArgumentException("limit must be between 1 and 10000");
        }
        String requestId = UUID.randomUUID().toString();
        KnowledgeIngestionCommand trusted = new KnowledgeIngestionCommand(command == null ? null : command.knowledgeBaseId(),
                command == null ? null : command.documentVersionId(), command == null ? null : command.limit(),
                command == null ? null : command.indexRevision(), user.tenantId(),
                internalCallSigner.sign("sms-web-bff", user.tenantId(), user.userId(), user.sessionId(), requestId));
        return ApiResponse.success(knowledgeService.ingest(trusted));
    }
}
