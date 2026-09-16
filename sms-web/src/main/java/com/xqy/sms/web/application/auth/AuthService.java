package com.xqy.sms.web.application.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xqy.sms.common.entity.SysUser;
import com.xqy.sms.common.entity.SysUserSession;
import com.xqy.sms.common.security.jwt.JwtTokenService;
import com.xqy.sms.common.security.jwt.JwtUserContext;
import com.xqy.sms.web.infrastructure.persistence.mapper.SysAuthorizationMapper;
import com.xqy.sms.web.infrastructure.persistence.mapper.SysUserMapper;
import com.xqy.sms.web.infrastructure.persistence.mapper.SysUserSessionMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthService {
    private static final String ENABLED = "ENABLED";
    private static final String ACTIVE = "ACTIVE";

    private final SysUserMapper userMapper;
    private final SysUserSessionMapper sessionMapper;
    private final SysAuthorizationMapper authorizationMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(SysUserMapper userMapper, SysUserSessionMapper sessionMapper,
                       SysAuthorizationMapper authorizationMapper, PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService) {
        this.userMapper = userMapper;
        this.sessionMapper = sessionMapper;
        this.authorizationMapper = authorizationMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public TokenPair login(String username, String password, String loginIp, String userAgent) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username).last("LIMIT 1"));
        if (user == null || !ENABLED.equals(user.getStatus())
                || !passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        LocalDateTime now = LocalDateTime.now();
        SysUserSession session = new SysUserSession();
        session.setUserId(user.getId());
        session.setLoginIp(loginIp);
        session.setUserAgent(userAgent);
        session.setDeviceType(resolveDeviceType(userAgent));
        session.setLoginTime(now);
        session.setExpireTime(now.plusSeconds(jwtTokenService.accessTokenTtlSeconds()));
        session.setRefreshExpireTime(now.plusSeconds(jwtTokenService.refreshTokenTtlSeconds()));
        session.setStatus(ACTIVE);
        sessionMapper.insert(session);
        JwtUserContext context = userContext(user.getId(), session.getId());
        String accessToken = jwtTokenService.createAccessToken(context);
        String refreshToken = jwtTokenService.createRefreshToken(context);
        session.setAccessToken(accessToken);
        session.setRefreshToken(refreshToken);
        sessionMapper.updateById(session);
        user.setLastLoginTime(now);
        user.setLastLoginIp(loginIp);
        userMapper.updateById(user);
        return new TokenPair(accessToken, refreshToken);
    }

    public TokenPair refresh(String refreshToken) {
        JwtUserContext context = jwtTokenService.verify(refreshToken);
        if (!JwtTokenService.REFRESH_TOKEN.equals(context.tokenType())) throw new InvalidCredentialsException();
        SysUserSession session = sessionMapper.selectById(context.sessionId());
        if (session == null || !ACTIVE.equals(session.getStatus()) || !refreshToken.equals(session.getRefreshToken())
                || session.getRefreshExpireTime().isBefore(LocalDateTime.now())
                || !context.userId().equals(session.getUserId())) throw new InvalidCredentialsException();
        SysUser user = userMapper.selectById(session.getUserId());
        if (user == null || !ENABLED.equals(user.getStatus())) throw new InvalidCredentialsException();
        JwtUserContext refreshedContext = userContext(session.getUserId(), session.getId());
        String accessToken = jwtTokenService.createAccessToken(refreshedContext);
        String nextRefreshToken = jwtTokenService.createRefreshToken(refreshedContext);
        LocalDateTime now = LocalDateTime.now();
        session.setAccessToken(accessToken);
        session.setRefreshToken(nextRefreshToken);
        session.setExpireTime(now.plusSeconds(jwtTokenService.accessTokenTtlSeconds()));
        session.setRefreshExpireTime(now.plusSeconds(jwtTokenService.refreshTokenTtlSeconds()));
        sessionMapper.updateById(session);
        return new TokenPair(accessToken, nextRefreshToken);
    }

    public AuthenticatedUser authenticate(String accessToken) {
        JwtUserContext context = jwtTokenService.verifyAccessToken(accessToken);
        SysUserSession session = sessionMapper.selectById(context.sessionId());
        if (session == null || !ACTIVE.equals(session.getStatus()) || !accessToken.equals(session.getAccessToken())
                || session.getExpireTime().isBefore(LocalDateTime.now())
                || !context.userId().equals(session.getUserId())) throw new InvalidCredentialsException();
        SysUser user = userMapper.selectById(session.getUserId());
        if (user == null || !ENABLED.equals(user.getStatus())) throw new InvalidCredentialsException();
        return new AuthenticatedUser(user, authorizationMapper.findRoleCodes(user.getId()),
                authorizationMapper.findButtonAuthorities(user.getId()));
    }

    public UserInfo userInfo(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new InvalidCredentialsException();
        return new UserInfo(authorizationMapper.findButtonAuthorities(userId), authorizationMapper.findRoleCodes(userId),
                user.getId(), user.getUsername(), user.getEmail(), user.getAvatar());
    }

    private String resolveDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "UNKNOWN";
        String normalized = userAgent.toLowerCase();
        return normalized.contains("mobile") || normalized.contains("android") || normalized.contains("iphone")
                ? "MOBILE" : "DESKTOP";
    }

    private JwtUserContext userContext(Long userId, Long sessionId) {
        return new JwtUserContext(userId, sessionId, null, authorizationMapper.findRoleCodes(userId),
                authorizationMapper.findButtonAuthorities(userId), List.of(), JwtTokenService.ACCESS_TOKEN);
    }

    public record TokenPair(String token, String refreshToken) { }
    public record AuthenticatedUser(SysUser user, List<String> roles, List<String> authorities) { }
    public record UserInfo(List<String> buttons, List<String> roles, Long userId, String userName, String email, String avatar) { }
    public static class InvalidCredentialsException extends RuntimeException { }
}
