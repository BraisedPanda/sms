package com.xqy.sms.ai.interfaces.dubbo;

import com.xqy.sms.ai.api.model.AiChatCommand;
import com.xqy.sms.ai.api.model.AiRunView;
import com.xqy.sms.ai.api.service.AiChatCommandService;
import com.xqy.sms.ai.application.service.conversation.ConversationApplicationService;
import com.xqy.sms.ai.application.service.run.AiTaskRunService;
import com.xqy.sms.ai.domain.model.AiTaskRequest;
import com.xqy.sms.common.security.jwt.JwtUserContext;
import com.xqy.sms.common.security.rpc.InternalCallContext;
import com.xqy.sms.common.security.rpc.InternalCallSigner;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@DubboService
public class AiChatCommandServiceImpl implements AiChatCommandService {
    private static final Logger log = LoggerFactory.getLogger(AiChatCommandServiceImpl.class);
    private final ConversationApplicationService conversation;
    private final AiTaskRunService runs;
    private final InternalCallSigner internalCallSigner;
    private final com.xqy.sms.ai.api.service.AiOperationsService operations;
    public AiChatCommandServiceImpl(ConversationApplicationService conversation, AiTaskRunService runs,
                                    @Value("${sms.internal-rpc.secret}") String internalRpcSecret,
                                    com.xqy.sms.ai.api.service.AiOperationsService operations) {
        this.conversation = conversation;
        this.runs = runs;
        this.internalCallSigner = new InternalCallSigner(internalRpcSecret);
        this.operations = operations;
    }
    @Override public void submit(AiChatCommand command) {
        submitRun(command);
    }
    @Override public java.util.Map<String, Object> submitRun(AiChatCommand command) {
        if (command == null) throw new IllegalArgumentException("chat command must not be null");
        InternalCallContext context = authenticate(command.callerContext());
        if (!java.util.Objects.equals(context.userId(), command.userId())
                || !java.util.Objects.equals(context.sessionId(), command.sessionId())
                || !java.util.Objects.equals(context.tenantId(), command.tenantId())) {
            throw new InternalCallSigner.InternalCallAuthenticationException();
        }
        AiTaskRequest request = new AiTaskRequest(); request.setQuestion(command.question()); request.setAlias(command.alias()); request.setIdempotencyKey(command.idempotencyKey());
        if (command.getConversationId() != null) operations.requireConversation(command.getConversationId(), context);
        request.setConversationId(command.getConversationId());
        log.info("rpc_audit action=chat_submit caller={} tenant={} user={} requestId={}", context.callerService(),
                context.tenantId(), context.userId(), context.requestId());
        com.xqy.sms.common.entity.AiTaskRun run = conversation.start(request, new JwtUserContext(command.userId(), command.sessionId(), context.requestId(),
                context.tenantId(), List.of(), List.of(), List.of(), "internal"), command.streamKey());
        java.util.Map<String, Object> receipt = new java.util.LinkedHashMap<>();
        receipt.put("runId", run.getRunId()); receipt.put("status", run.getStatus());
        receipt.put("conversationId", run.getConversationId() == null ? null : run.getConversationId().toString());
        return receipt;
    }
    @Override public void cancel(String runId, InternalCallContext callerContext) {
        InternalCallContext context = authenticate(callerContext);
        log.info("rpc_audit action=chat_cancel caller={} tenant={} user={} requestId={}", context.callerService(),
                context.tenantId(), context.userId(), context.requestId());
        conversation.cancel(runId, new JwtUserContext(context.userId(), context.sessionId(), context.requestId(), context.tenantId(), List.of(), List.of(), List.of(), "internal"));
    }
    @Override public AiRunView getRun(String runId, InternalCallContext callerContext) {
        InternalCallContext context = authenticate(callerContext);
        com.xqy.sms.common.entity.AiTaskRun run = runs.requireOwnedRun(runId, context.tenantId(),
                String.valueOf(context.userId()));
        log.info("rpc_audit action=run_get caller={} tenant={} user={} requestId={} runId={}",
                context.callerService(), context.tenantId(), context.userId(), context.requestId(), runId);
        return new AiRunView(run.getRunId(), run.getStatus(), run.getCurrentStepNo(), run.getStartTime(), run.getFinishTime());
    }
    private InternalCallContext authenticate(InternalCallContext context) {
        internalCallSigner.verify(context, "sms-web-bff");
        operations.requireCaller(context);
        return context;
    }
}
