package com.xqy.sms.ai.controller;

import com.xqy.sms.ai.service.AiChatAssistant;
import com.xqy.sms.common.dto.ApiResponse;
import com.xqy.sms.student.api.entity.StudentDTO;
import com.xqy.sms.student.api.service.StudentService;
import dev.langchain4j.model.openai.OpenAiChatModel;

import dev.langchain4j.service.AiServices;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    @DubboReference
    private StudentService studentService;

    private final AiChatAssistant aiChatAssistant;

    public AiChatController(OpenAiChatModel openAiChatModel) {
        this.aiChatAssistant = AiServices.create(AiChatAssistant.class, openAiChatModel);
    }

    @GetMapping("/test1")
    public ApiResponse<?> test1() {
        List<StudentDTO> list =  studentService.listStudents();
        return ApiResponse.success(list);
    }


    @GetMapping("/sample-chat")
    public ApiResponse<?> sampleChat(@RequestParam String question) {
        String answer = aiChatAssistant.chat(question);
        return ApiResponse.success(answer);
    }


}
