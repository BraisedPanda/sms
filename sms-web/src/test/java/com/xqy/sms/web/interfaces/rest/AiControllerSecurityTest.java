package com.xqy.sms.web.interfaces.rest;

import com.xqy.sms.ai.api.model.AiRunView;
import com.xqy.sms.ai.api.model.KnowledgeIngestionCommand;
import com.xqy.sms.ai.api.model.KnowledgeIngestionResult;
import com.xqy.sms.ai.api.service.AiChatCommandService;
import com.xqy.sms.ai.api.service.AiKnowledgeCommandService;
import com.xqy.sms.common.security.rpc.InternalCallContext;
import com.xqy.sms.common.security.rpc.InternalCallSigner;
import com.xqy.sms.web.application.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiControllerSecurityTest {
    private static final String SECRET = "01234567890123456789012345678901";
    private final AuthService.AuthenticatedUser user = new AuthService.AuthenticatedUser(
            10L, 20L, "token-1", "tenant-1", List.of("R_ADMIN"), List.of());
    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(user, null, List.of());

    @Test
    void runQueryUsesSignedAuthenticatedScope() {
        AiChatCommandService service = mock(AiChatCommandService.class);
        AiRunView view = new AiRunView("run-1", "RUNNING", 1, null, null);
        when(service.getRun(eq("run-1"), any())).thenAnswer(invocation -> {
            InternalCallContext context = invocation.getArgument(1);
            new InternalCallSigner(SECRET).verify(context, "sms-web-bff");
            assertEquals("tenant-1", context.tenantId());
            assertEquals(10L, context.userId());
            return view;
        });
        AiChatController controller = new AiChatController(mock(StringRedisTemplate.class), SECRET);
        ReflectionTestUtils.setField(controller, "chatService", service);

        assertEquals(view, controller.getRun("run-1", authentication).getData());
        verify(service).getRun(eq("run-1"), any());
    }

    @Test
    void cancelRejectsInvalidRunId() {
        AiChatController controller = new AiChatController(mock(StringRedisTemplate.class), SECRET);
        assertThrows(IllegalArgumentException.class, () -> controller.cancel(" ", authentication));
    }

    @Test
    void ingestionIgnoresUntrustedTenantFromBody() {
        AiKnowledgeCommandService service = mock(AiKnowledgeCommandService.class);
        when(service.ingest(any())).thenAnswer(invocation -> {
            KnowledgeIngestionCommand command = invocation.getArgument(0);
            assertEquals("tenant-1", command.tenantId());
            new InternalCallSigner(SECRET).verify(command.callerContext(), "sms-web-bff");
            return new KnowledgeIngestionResult(1, 1, 1);
        });
        AiKnowledgeController controller = new AiKnowledgeController(SECRET);
        ReflectionTestUtils.setField(controller, "knowledgeService", service);
        KnowledgeIngestionCommand untrusted = new KnowledgeIngestionCommand(1L, 2L, 10, 1,
                "tenant-2", (InternalCallContext) null);

        assertEquals(1, controller.ingest(untrusted, authentication).getData().imported());
    }

    @Test
    void ingestionRejectsOutOfRangeLimit() {
        AiKnowledgeController controller = new AiKnowledgeController(SECRET);
        assertThrows(IllegalArgumentException.class,
                () -> controller.ingest(new KnowledgeIngestionCommand(null, null, 10_001, 1), authentication));
    }
}
