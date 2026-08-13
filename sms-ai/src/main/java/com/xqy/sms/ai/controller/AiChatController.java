package com.xqy.sms.ai.controller;

import com.xqy.sms.common.dto.ApiResponse;
import com.xqy.sms.student.api.entity.StudentDTO;
import com.xqy.sms.student.api.service.StudentService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    @DubboReference
    private StudentService studentService;

    @GetMapping("/test1")
    public ApiResponse<?> test1() {
        List<StudentDTO> list =  studentService.listStudents();
        return ApiResponse.success(list);
    }


}
