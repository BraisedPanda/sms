package com.xqy.sms.ai.application.service.chat;

import com.xqy.sms.ai.application.event.AiStreamEventPublisher;
import com.xqy.sms.ai.infrastructure.cache.RedisChatMemoryStore;
import com.xqy.sms.ai.infrastructure.model.*;
import com.xqy.sms.ai.infrastructure.service.log.AiRequestLogService;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.*;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AiChatCancellationTest {
    @Test void cancelsProviderHandleAndIgnoresLateSuccessOrTokens() {
        var registry=mock(ModelRegistry.class);
        var handler=new AtomicReference<StreamingChatResponseHandler>();
        var request=new AtomicReference<ChatRequest>();
        StreamingChatModel model=new StreamingChatModel() {
            public void doChat(ChatRequest input,StreamingChatResponseHandler callback) { request.set(input); handler.set(callback); }
        };
        when(registry.find(anyString())).thenReturn(Optional.of(new ModelHandle(mock(ChatModel.class),model)));
        var memory=mock(RedisChatMemoryStore.class);
        Map<Object,List<ChatMessage>> memories=new HashMap<>();
        when(memory.getMessages(any())).thenAnswer(call->memories.getOrDefault(call.getArgument(0),List.of()));
        doAnswer(call->{memories.put(call.getArgument(0),new ArrayList<>(call.getArgument(1))); return null;}).when(memory).updateMessages(any(),anyList());
        doAnswer(call->{memories.putIfAbsent(call.getArgument(0),call.getArgument(1)); return null;}).when(memory).initializeIfAbsent(any(),anyList());
        var events=mock(AiStreamEventPublisher.class);
        var log=mock(AiRequestLogService.class);
        var service=new AiChatService(registry,memory,log,events,20);
        var success=new AtomicReference<String>();
        var failure=new AtomicReference<Throwable>();
        service.answer("stream","new question","conversation","request","balanced",success::set,failure::set,()->false,
                List.of(UserMessage.from("old question"),AiMessage.from("old answer")));
        assertNotNull(handler.get());
        assertTrue(request.get().messages().stream().anyMatch(m->m.equals(AiMessage.from("old answer"))));
        var handle=mock(StreamingHandle.class);
        handler.get().onPartialResponse(new PartialResponse("first"),new PartialResponseContext(handle));
        service.cancel("stream");
        verify(handle).cancel();
        assertNotNull(failure.get());
        handler.get().onPartialResponse(new PartialResponse("late"),new PartialResponseContext(handle));
        handler.get().onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("late complete")).build());
        assertNull(success.get());
        verify(events,never()).publish("stream","token","late");
        verify(events,never()).publish(eq("stream"),eq("done"),any());
    }
}
