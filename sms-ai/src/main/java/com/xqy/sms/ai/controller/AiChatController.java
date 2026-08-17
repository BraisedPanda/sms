package com.xqy.sms.ai.controller;

import com.xqy.sms.ai.service.AiAssistantService;
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


    private final AiAssistantService aiAssistantService;

    public AiChatController(AiAssistantService aiAssistantService) {

        this.aiAssistantService = aiAssistantService;
    }

    @GetMapping("/test1")
    public ApiResponse<?> test1() {
        List<Student> list = studentService.listStudents();
        return ApiResponse.success(list);
    }


    @GetMapping("/sample-chat")
    public ApiResponse<?> sampleChat(@RequestParam String question) {
        String answer = aiAssistantService.sampleChat(question);
        return ApiResponse.success(answer);
    }


    @PostMapping("/chat")
    public SseEmitter chat(@RequestParam String question) {
        return aiAssistantService.chat(question);

    }


}
