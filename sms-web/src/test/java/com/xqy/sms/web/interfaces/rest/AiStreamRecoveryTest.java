package com.xqy.sms.web.interfaces.rest;

import com.xqy.sms.ai.api.service.*;
import com.xqy.sms.common.security.rpc.InternalCallSigner;
import com.xqy.sms.web.application.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AiStreamRecoveryTest {
    static final String SECRET="01234567890123456789012345678901";
    final UsernamePasswordAuthenticationToken auth=new UsernamePasswordAuthenticationToken(
            new AuthService.AuthenticatedUser(10L,20L,"token","tenant",List.of(),List.of()),null,List.of());
    @Test void submissionReturnsRunReceiptAndUsesTrustedIdentity() {
        var chat=mock(AiChatCommandService.class);
        when(chat.submitRun(any())).thenAnswer(call->{
            com.xqy.sms.ai.api.model.AiChatCommand input=call.getArgument(0);
            new InternalCallSigner(SECRET).verify(input.callerContext(),"sms-web-bff");
            assertEquals(10L,input.userId()); assertEquals("tenant",input.tenantId()); assertEquals(55L,input.getConversationId());
            return Map.of("runId","run","status","PENDING");
        });
        var controller=new AiChatController(mock(StringRedisTemplate.class),SECRET);
        ReflectionTestUtils.setField(controller,"chatService",chat);
        assertEquals("run",controller.start(new AiChatController.ChatRequest(" question ","balanced","key",55L),auth).getData().get("runId"));
        assertThrows(IllegalArgumentException.class,()->controller.start(new AiChatController.ChatRequest("x".repeat(4001),"balanced","key"),auth));
    }
    @Test @SuppressWarnings({"unchecked","rawtypes"})
    void replaysRedisIdsFromCursorAndRetainsStreamForAnotherConnection() throws Exception {
        var redis=mock(StringRedisTemplate.class);
        StreamOperations operations=mock(StreamOperations.class);
        when(redis.opsForStream()).thenReturn(operations);
        var records=List.of(
                MapRecord.create("stream",Map.of("event","token","data","你好")).withId(RecordId.of("2-0")),
                MapRecord.create("stream",Map.of("event","done","data","完成")).withId(RecordId.of("3-0")));
        when(operations.read(any(StreamReadOptions.class),any(StreamOffset[].class))).thenAnswer(call->{
            StreamOffset offset=call.getArgument(1);
            assertEquals("1-0",offset.getOffset().getOffset());
            return records;
        });
        var queries=mock(AiOperationsService.class);
        when(queries.delivery(eq("run"),any())).thenReturn(Map.of("streamKey","stream","status","RUNNING"));
        var controller=new AiChatController(redis,SECRET);
        ReflectionTestUtils.setField(controller,"operations",queries);
        var mvc=MockMvcBuilders.standaloneSetup(controller).build();
        var pending=mvc.perform(get("/api/ai/runs/run/events").param("after","1-0").principal(auth)).andExpect(request().asyncStarted()).andReturn();
        pending.getAsyncResult(3000);
        var body=mvc.perform(asyncDispatch(pending)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(body.contains("id:2-0"),body); assertTrue(body.contains("你好"),body); assertTrue(body.contains("event:done"),body);
        verify(redis,never()).delete(anyString());
    }
    @Test @SuppressWarnings({"unchecked","rawtypes"})
    void expiredEventStreamRestoresDurableAnswerAndCompletes() throws Exception {
        var redis=mock(StringRedisTemplate.class);
        StreamOperations operations=mock(StreamOperations.class);
        when(redis.opsForStream()).thenReturn(operations);
        when(operations.read(any(StreamReadOptions.class),any(StreamOffset[].class))).thenReturn(List.of());
        var queries=mock(AiOperationsService.class);
        when(queries.delivery(eq("run"),any())).thenReturn(Map.of("streamKey","stream","status","SUCCEEDED","answer","历史完整回答"));
        var controller=new AiChatController(redis,SECRET);
        ReflectionTestUtils.setField(controller,"operations",queries);
        var mvc=MockMvcBuilders.standaloneSetup(controller).build();
        var pending=mvc.perform(get("/api/ai/runs/run/events").principal(auth)).andExpect(request().asyncStarted()).andReturn();
        pending.getAsyncResult(3000);
        var body=mvc.perform(asyncDispatch(pending)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(body.contains("event:snapshot"),body); assertTrue(body.contains("历史完整回答"),body); assertTrue(body.contains("event:done"),body);
        assertThrows(IllegalArgumentException.class,()->controller.events("run","forged-cursor",auth));
    }
}
