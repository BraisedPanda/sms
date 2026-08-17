package com.xqy.sms.ai.service;

import com.xqy.sms.ai.service.assistant.AiChatAssistant;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class AiAssistantService {


    private final AiChatAssistant aiChatAssistant;

    public AiAssistantService(OpenAiChatModel openAiChatModel,
                              StreamingChatModel streamingChatModel) {
        this.aiChatAssistant = AiServices.builder(AiChatAssistant.class)
                .chatModel(openAiChatModel)
                .streamingChatModel(streamingChatModel)
                .build();

    }

    public String sampleChat(String question) {
        return aiChatAssistant.sampleChat(question);
    }

    public SseEmitter chat(String question) {
        SseEmitter emitter = new SseEmitter(0L);
        TokenStream tokenStream;

        try {
            tokenStream = aiChatAssistant.chat(question);
            tokenStream
                    .onPartialResponse(token -> sendToken(emitter, token))
                    .onCompleteResponse(response -> emitter.complete())
                    .onError(emitter::completeWithError)
                    .start();
        } catch (RuntimeException exception) {
            emitter.completeWithError(exception);
        }

        return emitter;
    }

    private void sendToken(SseEmitter emitter, String token) {
        try {
            emitter.send(SseEmitter.event().data(token));
        } catch (Exception exception) {
            emitter.completeWithError(exception);
        }
    }
}
