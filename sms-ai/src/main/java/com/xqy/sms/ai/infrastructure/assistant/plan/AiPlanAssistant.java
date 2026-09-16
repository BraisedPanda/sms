package com.xqy.sms.ai.infrastructure.assistant.plan;

import dev.langchain4j.service.UserMessage;

public interface AiPlanAssistant {


    String plan(@UserMessage String prompt);

}
