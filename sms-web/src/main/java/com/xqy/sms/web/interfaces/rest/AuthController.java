package com.xqy.sms.web.interfaces.rest;

import com.xqy.sms.common.dto.ApiResponse;
import com.xqy.sms.web.application.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import com.xqy.sms.system.api.model.SystemAuthModels;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
        return ApiResponse.success(authService.userInfo(principal(authentication).userId()));
    }

    @PostMapping("/auth/logout")
    public ApiResponse<Void> logout(Authentication authentication) {
        AuthService.AuthenticatedUser user = principal(authentication);
        authService.logout(user.sessionId(), user.tokenId());
        return ApiResponse.success(null);
    }

    @PostMapping("/system/sessions/{sessionId}/kick")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('R_SUPER', 'R_ADMIN')")
    public ApiResponse<Void> kick(@PathVariable Long sessionId) {
        authService.kick(sessionId);
        return ApiResponse.success(null);
    }

    @GetMapping("/user/list")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('user:read')")
    public ApiResponse<SystemAuthModels.UserPage> users(String userName, String status, Long current, Long size) {
        return ApiResponse.success(authService.users(userName, status, current == null ? 1 : current, size == null ? 10 : size));
    }

    @GetMapping("/v3/system/menus")
    public ApiResponse<List<Map<String, Object>>> menus(Authentication authentication) {
        return ApiResponse.success(menuTree(authService.menus(principal(authentication).userId())));
    }

    private AuthService.AuthenticatedUser principal(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthService.AuthenticatedUser user) return user;
        throw new AuthService.InvalidCredentialsException();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> menuTree(List<SystemAuthModels.Menu> menus) {
        Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
        for (SystemAuthModels.Menu menu : menus) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("path", menu.path()); item.put("name", menu.name()); item.put("component", menu.component()); item.put("redirect", menu.redirect());
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("title", menu.title()); meta.put("icon", menu.icon()); meta.put("keepAlive", Boolean.TRUE.equals(menu.keepAlive()));
            meta.put("isHide", !Boolean.TRUE.equals(menu.visible())); meta.put("isHideTab", Boolean.TRUE.equals(menu.hideTab()));
            meta.put("isFullPage", Boolean.TRUE.equals(menu.fullPage())); meta.put("link", menu.link()); meta.put("isIframe", Boolean.TRUE.equals(menu.iframe())); meta.put("authList", menu.authList());
            item.put("meta", meta); item.put("children", new java.util.ArrayList<Map<String, Object>>()); byId.put(menu.id(), item);
        }
        List<Map<String, Object>> roots = new java.util.ArrayList<>();
        for (SystemAuthModels.Menu menu : menus) { Map<String, Object> item = byId.get(menu.id()); Map<String, Object> parent = byId.get(menu.parentId()); if (parent == null) roots.add(item); else ((List<Map<String, Object>>) parent.get("children")).add(item); }
        return roots;
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }

    public record LoginRequest(String userName, String password) { }
    public record RefreshRequest(String refreshToken) { }
}
