package com.xqy.sms.ai.controller;

import com.xqy.sms.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary in-memory authentication for the local sms-ui demo.
 *
 * <p>This is deliberately not a production authentication mechanism. Tokens
 * are lost when the application restarts and there is no persistence or
 * password hashing. Replace this controller when the real identity service is
 * available.</p>
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TempAuthController {

    private static final String TEMP_PASSWORD = "123456";
    private static final Map<String, TempUser> USERS = Map.of(
            "super", new TempUser(1L, "Super", "super@example.com", List.of("R_SUPER")),
            "admin", new TempUser(2L, "Admin", "admin@example.com", List.of("R_ADMIN")),
            "user", new TempUser(3L, "User", "user@example.com", List.of("R_USER"))
    );

    private final Map<String, TempUser> activeTokens = new ConcurrentHashMap<>();

    @PostMapping("/auth/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        if (request == null || request.userName() == null || request.password() == null) {
            return new ApiResponse<>(400, "用户名和密码不能为空", null);
        }

        String username = request.userName().trim().toLowerCase(Locale.ROOT);
        TempUser user = USERS.get(username);
        if (user == null || !TEMP_PASSWORD.equals(request.password())) {
            return new ApiResponse<>(401, "用户名或密码错误", null);
        }

        String token = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString();
        activeTokens.put(token, user);
        return ApiResponse.success(new LoginResponse(token, refreshToken));
    }

    @GetMapping("/user/info")
    public ApiResponse<UserInfo> userInfo(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        TempUser user = activeTokens.get(normalizeToken(authorization));
        if (user == null) {
            return new ApiResponse<>(401, "登录已失效，请重新登录", null);
        }

        return ApiResponse.success(new UserInfo(
                List.of("add", "edit", "delete", "export"),
                user.roles(),
                user.userId(),
                user.userName(),
                user.email(),
                null
        ));
    }

    private String normalizeToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return "";
        }
        String value = authorization.trim();
        return value.regionMatches(true, 0, "Bearer ", 0, 7)
                ? value.substring(7).trim()
                : value;
    }

    public record LoginRequest(String userName, String password) {
    }

    public record LoginResponse(String token, String refreshToken) {
    }

    public record UserInfo(List<String> buttons, List<String> roles, long userId,
                           String userName, String email, String avatar) {
    }

    private record TempUser(long userId, String userName, String email, List<String> roles) {
    }
}
