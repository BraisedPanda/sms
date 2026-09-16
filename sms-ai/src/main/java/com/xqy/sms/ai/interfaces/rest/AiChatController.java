package com.xqy.sms.ai.interfaces.rest;

import com.xqy.sms.ai.domain.model.AiConstants;
import com.xqy.sms.ai.domain.model.AiTaskRequest;
import com.xqy.sms.ai.application.service.chat.AiChatService;
import com.xqy.sms.ai.application.service.conversation.ConversationApplicationService;
import com.xqy.sms.ai.application.service.run.AiTaskRunService;
import com.xqy.sms.common.dto.ApiResponse;
import com.xqy.sms.common.security.jwt.JwtUserContext;
import com.xqy.sms.student.api.entity.Student;
import com.xqy.sms.student.api.service.StudentService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
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
    public SseEmitter chat(@RequestBody AiTaskRequest aiTaskRequest, Authentication authentication) {
        aiTaskRequest.setAlias(AiConstants.MODEL_ALIAS.BALANCED);
        return conversationApplicationService.start(aiTaskRequest, currentUser(authentication));

    }

    @PostMapping("/runs/{runId}/cancel")
    public ApiResponse<Void> cancel(@PathVariable String runId, Authentication authentication) {
        try {
            conversationApplicationService.cancel(runId, currentUser(authentication));
            return ApiResponse.success(null);
        } catch (AiTaskRunService.RunOwnershipException exception) {
            return new ApiResponse<>(403, "无权取消其他用户的 AI 任务", null);
        }
    }

    private JwtUserContext currentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof JwtUserContext context) return context;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未认证的访问请求");
    }

}
