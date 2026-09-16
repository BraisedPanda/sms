package com.xqy.sms.web.controller;

import com.xqy.sms.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/web")
public class SmsWebController {

    @GetMapping("/hello")
    @PreAuthorize("hasAuthority('web:hello')")
    public ApiResponse<String> hello() {
        return ApiResponse.success("Hello from sms-web");
    }
}
