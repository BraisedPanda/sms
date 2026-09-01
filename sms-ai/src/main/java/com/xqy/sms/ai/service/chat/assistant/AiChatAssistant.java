package com.xqy.sms.ai.service.chat.assistant;


import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.MemoryId;

public interface AiChatAssistant {

    String sampleChat( String question);

    TokenStream chat(@MemoryId String memoryId, String question);

    TokenStream answer(@MemoryId String memoryId, String resultJson);
}
