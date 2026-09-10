package com.xqy.sms.ai.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

/** Synchronous and streaming models built from one database definition. */
public record ModelHandle(ChatModel chatModel, StreamingChatModel streamingChatModel) {

    public ChatModel chatModel() {
        return chatModel;
    }
}
