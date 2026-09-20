package com.xqy.sms.web.application.auth;

import com.xqy.sms.system.api.model.SystemAuthModels;
import com.xqy.sms.system.api.service.SystemAuthService;
import com.xqy.sms.system.api.service.SystemAuthenticationException;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import com.xqy.sms.common.security.jwt.JwtTokenService;
import com.xqy.sms.common.security.jwt.JwtUserContext;

@Service
public class AuthService {
    @DubboReference(check = false)
    private SystemAuthService systemAuthService;
    private final JwtTokenService jwtTokenService;

    public AuthService(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }
    public TokenPair login(String username, String password, String ip, String userAgent) {
        try {
            SystemAuthModels.TokenPair pair = systemAuthService.login(username, password, ip, userAgent);
            return new TokenPair(pair.token(), pair.refreshToken());
        } catch (SystemAuthenticationException exception) {
            throw new InvalidCredentialsException();
        }
    }
    public TokenPair refresh(String token) { SystemAuthModels.TokenPair pair = systemAuthService.refresh(token); return new TokenPair(pair.token(), pair.refreshToken()); }
    public AuthenticatedUser authenticate(String token) {
        JwtUserContext context = jwtTokenService.verifyAccessToken(token);
        SystemAuthModels.Principal principal = systemAuthService.authenticate(context.userId(), context.sessionId(), context.tokenId());
        if (!java.util.Objects.equals(context.tenantId(), principal.tenantId())) throw new InvalidCredentialsException();
        return new AuthenticatedUser(principal.userId(), principal.sessionId(), principal.tokenId(), principal.tenantId(),
                principal.roles(), principal.authorities());
    }
    public UserInfo userInfo(Long userId) { SystemAuthModels.UserInfo info = systemAuthService.userInfo(userId); return new UserInfo(info.buttons(), info.roles(), info.userId(), info.userName(), info.email(), info.avatar()); }
    public void logout(Long sessionId, String tokenId) { systemAuthService.logout(sessionId, tokenId); }
    public void kick(Long sessionId) { systemAuthService.kick(sessionId); }
    public SystemAuthModels.UserPage users(String username, String status, long current, long size) { return systemAuthService.users(username, status, current, size); }
    public java.util.List<SystemAuthModels.Menu> menus(Long userId) { return systemAuthService.menus(userId); }
    public record TokenPair(String token, String refreshToken) { }
    public record AuthenticatedUser(Long userId, Long sessionId, String tokenId, String tenantId,
                                    java.util.List<String> roles, java.util.List<String> authorities) { }
    public record UserInfo(java.util.List<String> buttons, java.util.List<String> roles, Long userId, String userName, String email, String avatar) { }
    public static class InvalidCredentialsException extends RuntimeException { }
}
