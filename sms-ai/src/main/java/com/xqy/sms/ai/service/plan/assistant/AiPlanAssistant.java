package com.xqy.sms.ai.service.plan.assistant;

import dev.langchain4j.service.UserMessage;

public interface AiPlanAssistant {


    String plan(@UserMessage String prompt);

}
