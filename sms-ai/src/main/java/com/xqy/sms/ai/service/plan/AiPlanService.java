package com.xqy.sms.ai.service.plan;

import com.xqy.sms.ai.model.AiConstants;
import com.xqy.sms.ai.model.AiTask;
import com.xqy.sms.ai.model.AiTaskRequest;
import com.xqy.sms.ai.service.chat.AiChatService;
import com.xqy.sms.ai.service.conversation.ConversationApplicationService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/** Compatibility facade for existing callers; lifecycle work lives in dedicated services. */
@Component
public class AiPlanService {
    private final TaskPlannerService plannerService;
    private final ConversationApplicationService conversationService;
    private final AiChatService chatService;

    public AiPlanService(TaskPlannerService plannerService, ConversationApplicationService conversationService,
                         AiChatService chatService) {
        this.plannerService = plannerService;
        this.conversationService = conversationService;
        this.chatService = chatService;
    }

    public String sampleChat(String question) { return chatService.sampleChat(question); }
    public SseEmitter chat(AiTaskRequest request) { return conversationService.start(request); }
    public List<AiTask> planTasks(String question, String businessContext) {
        return planTasks(question, businessContext, AiConstants.MODEL_ALIAS.STRONG);
    }
    public List<AiTask> planTasks(String question, String businessContext, String alias) {
        return plannerService.plan(question, businessContext, alias);
    }
}
