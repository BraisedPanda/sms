package com.xqy.sms.ai.application.service.plan;

import com.xqy.sms.ai.domain.model.AiConstants;
import com.xqy.sms.ai.domain.model.AiTask;
import com.xqy.sms.ai.application.service.chat.AiChatService;
import org.springframework.stereotype.Component;

import java.util.List;

/** Compatibility facade for existing callers; lifecycle work lives in dedicated services. */
@Component
public class AiPlanService {
    private final TaskPlannerService plannerService;
    private final AiChatService chatService;

    public AiPlanService(TaskPlannerService plannerService, AiChatService chatService) {
        this.plannerService = plannerService;
        this.chatService = chatService;
    }

    public String sampleChat(String question) { return chatService.sampleChat(question); }
    public List<AiTask> planTasks(String question, String businessContext) {
        return planTasks(question, businessContext, AiConstants.MODEL_ALIAS.STRONG);
    }
    public List<AiTask> planTasks(String question, String businessContext, String alias) {
        return plannerService.plan(question, businessContext, alias);
    }
}
