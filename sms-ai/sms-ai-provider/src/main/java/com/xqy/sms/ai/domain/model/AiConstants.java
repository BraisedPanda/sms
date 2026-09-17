package com.xqy.sms.ai.domain.model;

public interface AiConstants {

    interface MODEL_ALIAS {
        String BALANCED = "balanced";
        String STRONG = "strong";
    }

    interface MODEL_PROVIDER {
        String OPENAI = "openai";
        String DEEPSEEK = "deepseek";
        String OPEN_AI = "open-ai";
    }

    interface TASK_DOMAIN {
        String CHAT = "chat";
    }

    interface SSE_EVENT {
        String START = "start";
        String PLANNING = "planning";
        String EXECUTING = "executing";
        String GENERATING = "generating";
        String TOKEN = "token";
        String DONE = "done";
        String ERROR = "error";
    }

    interface CACHE_KEY {
        String CHAT_PREFIX = "chat_";
        String BUSINESS_PREFIX = "business_";
    }

    interface PROMPT_CODE {
        String PLAN = "AI_PLAN";
    }

}
