package com.xqy.sms.ai.controller;

import com.xqy.sms.ai.model.AiTaskRequest;
import com.xqy.sms.ai.service.plan.AiPlanService;
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


    private final AiPlanService aiPlanService;

    public AiChatController(AiPlanService aiPlanService) {

        this.aiPlanService = aiPlanService;
    }

    @GetMapping("/test1")
    public ApiResponse<?> test1() {
        List<Student> list = studentService.listStudents();
        return ApiResponse.success(list);
    }


    @GetMapping("/sample-chat")
    public ApiResponse<?> sampleChat(@RequestParam String question) {
        String answer = aiPlanService.sampleChat(question);
        return ApiResponse.success(answer);
    }


    @PostMapping("/chat")
    public SseEmitter chat(@RequestBody AiTaskRequest aiTaskRequest) {
        return aiPlanService.chat(aiTaskRequest);

    }


}
