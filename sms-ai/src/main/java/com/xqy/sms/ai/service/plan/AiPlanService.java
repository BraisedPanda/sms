package com.xqy.sms.ai.service.plan;

import com.xqy.sms.ai.model.AiRequest;
import com.xqy.sms.ai.service.chat.assistant.AiChatAssistant;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PreDestroy;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class AiPlanService {


    private final AiChatAssistant aiChatAssistant;

    private final ThreadPoolExecutor planningExecutor;

    private final static int CORE_POOL_SIZE = 2;
    private final static int MAX_POOL_SIZE = 10;
    private final static long KEEP_ALIVE_TIME = 60L;

    private final static String CHAT_PREFIX = "chat_";

    private final static String BUSINESS_PREFIX = "business_";


    public AiPlanService(OpenAiChatModel openAiChatModel,
                         StreamingChatModel streamingChatModel) {
        this.aiChatAssistant = AiServices.builder(AiChatAssistant.class)
                .chatModel(openAiChatModel)
                .streamingChatModel(streamingChatModel)
                .build();

        this.planningExecutor =  new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
    }


    @PreDestroy
    public void preDestroy() {
        planningExecutor.shutdown();
    }



    public String sampleChat(String question) {
        return aiChatAssistant.sampleChat(question);
    }

    public SseEmitter chat(AiRequest aiRequest) {
        SseEmitter emitter = createNewSseEmitter();
        sendEvent(emitter, "start", "Chat started");
        try {
            planningExecutor.execute(() -> startChat(emitter, aiRequest));
        }catch (Exception e){
            sendEvent(emitter, "error", e.getMessage());
            emitter.completeWithError(e);
        }
        return emitter;
    }


    public void startChat(SseEmitter emitter, AiRequest aiRequest) {

        String businessMemoryId = buildMemoryId(BUSINESS_PREFIX, aiRequest);
        String chatMemoryId = buildMemoryId(CHAT_PREFIX, aiRequest);


    }


    private void sendToken(SseEmitter emitter, String token) {
        try {
            emitter.send(SseEmitter.event().data(token));
        } catch (Exception exception) {
            emitter.completeWithError(exception);
        }
    }




    public SseEmitter createNewSseEmitter() {
        return new SseEmitter(120L);
    }

    public void sendEvent(SseEmitter emitter, String status, Object data) {
        try {
            emitter.send(SseEmitter.event().name(status).data(data));
        } catch (Exception exception) {
            emitter.completeWithError(exception);
        }
    }

    public String buildMemoryId(String prefix, AiRequest aiRequest) {
        return  prefix + aiRequest.getUserId() + "_" + aiRequest.getSessionId();
    }
}
