package com.xqy.sms.ai.interfaces.dubbo;

import com.xqy.sms.ai.api.model.AiChatCommand;
import com.xqy.sms.ai.api.service.AiChatCommandService;
import com.xqy.sms.ai.application.service.conversation.ConversationApplicationService;
import com.xqy.sms.ai.application.service.run.AiTaskRunService;
import com.xqy.sms.ai.domain.model.AiTaskRequest;
import com.xqy.sms.common.security.jwt.JwtUserContext;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService
public class AiChatCommandServiceImpl implements AiChatCommandService {
    private final ConversationApplicationService conversation;
    private final AiTaskRunService runs;
    public AiChatCommandServiceImpl(ConversationApplicationService conversation, AiTaskRunService runs) { this.conversation = conversation; this.runs = runs; }
    @Override public void submit(AiChatCommand command) {
        AiTaskRequest request = new AiTaskRequest(); request.setQuestion(command.question()); request.setAlias(command.alias()); request.setIdempotencyKey(command.idempotencyKey());
        conversation.start(request, new JwtUserContext(command.userId(), command.sessionId(), "bff", null, List.of(), List.of(), List.of(), "access"), command.streamKey());
    }
    @Override public void cancel(String runId, Long userId) { runs.cancel(runId, String.valueOf(userId)); }
}
