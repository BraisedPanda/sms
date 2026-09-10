package com.xqy.sms.ai.model;

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

    String OPENAI_DEFAULT_BASE_URL = "https://api.openai.com/v1";
    long SSE_EMITTER_TIMEOUT_MILLIS = 120_000L;
    int CHAT_MEMORY_MAX_MESSAGES = 40;
    int BUSINESS_RESULT_TTL_MINUTES = 30;
}
