package com.xqy.sms.ai.service.plan.assistant;

import dev.langchain4j.service.V;

public interface AiPlanAssistant {


    String plan(@V("prompt") String prompt);

}
