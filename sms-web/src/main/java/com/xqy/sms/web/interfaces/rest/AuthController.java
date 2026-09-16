package com.xqy.sms.web.interfaces.rest;

import com.xqy.sms.common.dto.ApiResponse;
import com.xqy.sms.web.application.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/login")
    public ApiResponse<AuthService.TokenPair> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        if (request == null || blank(request.userName()) || blank(request.password())) {
            return new ApiResponse<>(400, "用户名和密码不能为空", null);
        }
        try {
            return ApiResponse.success(authService.login(request.userName().trim(), request.password(),
                    clientIp(httpRequest), httpRequest.getHeader("User-Agent")));
        } catch (AuthService.InvalidCredentialsException exception) {
            return new ApiResponse<>(401, "用户名或密码错误", null);
        }
    }

    @PostMapping("/auth/refresh")
    public ApiResponse<AuthService.TokenPair> refresh(@RequestBody RefreshRequest request) {
        if (request == null || blank(request.refreshToken())) return new ApiResponse<>(400, "刷新令牌不能为空", null);
        try {
            return ApiResponse.success(authService.refresh(request.refreshToken()));
        } catch (RuntimeException exception) {
            return new ApiResponse<>(401, "登录已失效，请重新登录", null);
        }
    }

    @GetMapping("/user/info")
    public ApiResponse<AuthService.UserInfo> userInfo(Authentication authentication) {
        return ApiResponse.success(authService.userInfo((Long) authentication.getPrincipal()));
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }

    public record LoginRequest(String userName, String password) { }
    public record RefreshRequest(String refreshToken) { }
}
