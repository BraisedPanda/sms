package com.xqy.sms.ai.service.chat.assistant;


import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

public interface AiChatAssistant {

    String sampleChat(@UserMessage String question);

    TokenStream chat(@MemoryId String memoryId, @UserMessage String question);

    TokenStream answer(@MemoryId String memoryId, @UserMessage String resultJson);
}
