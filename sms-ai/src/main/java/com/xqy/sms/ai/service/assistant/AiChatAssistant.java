package com.xqy.sms.ai.service.assistant;


import dev.langchain4j.service.TokenStream;

public interface AiChatAssistant {

    String sampleChat( String question);

    TokenStream chat(String question);
}
