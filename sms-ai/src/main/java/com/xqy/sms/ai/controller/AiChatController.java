package com.xqy.sms.ai.controller;

import com.xqy.sms.ai.model.AiConstants;
import com.xqy.sms.ai.model.AiTaskRequest;
import com.xqy.sms.ai.service.chat.AiChatService;
import com.xqy.sms.ai.service.conversation.ConversationApplicationService;
import com.xqy.sms.common.dto.ApiResponse;
import com.xqy.sms.student.api.entity.Student;
import com.xqy.sms.student.api.service.StudentService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    @DubboReference
    private StudentService studentService;


    private final ConversationApplicationService conversationApplicationService;
    private final AiChatService aiChatService;

    public AiChatController(ConversationApplicationService conversationApplicationService, AiChatService aiChatService) {

        this.conversationApplicationService = conversationApplicationService;
        this.aiChatService = aiChatService;
    }

    @GetMapping("/test1")
    public ApiResponse<?> test1() {
        List<Student> list = studentService.listStudents();
        return ApiResponse.success(list);
    }


    @GetMapping("/sample-chat")
    public ApiResponse<?> sampleChat(@RequestParam String question) {
        return ApiResponse.success(aiChatService.sampleChat(question));
    }


    @PostMapping("/chat")
    public SseEmitter chat(@RequestBody AiTaskRequest aiTaskRequest) {
        aiTaskRequest.setAlias(AiConstants.MODEL_ALIAS.BALANCED);
        return conversationApplicationService.start(aiTaskRequest);

    }

    @PostMapping("/runs/{runId}/cancel")
    public ApiResponse<Void> cancel(@PathVariable String runId) {
        conversationApplicationService.cancel(runId);
        return ApiResponse.success(null);
    }


}
