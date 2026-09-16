package com.xqy.sms.web.application.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xqy.sms.common.entity.SysUser;
import com.xqy.sms.common.entity.SysUserSession;
import com.xqy.sms.web.infrastructure.persistence.mapper.SysAuthorizationMapper;
import com.xqy.sms.web.infrastructure.persistence.mapper.SysUserMapper;
import com.xqy.sms.web.infrastructure.persistence.mapper.SysUserSessionMapper;
import io.jsonwebtoken.Claims;
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
        String accessToken = jwtTokenService.createAccessToken(user.getId(), session.getId());
        String refreshToken = jwtTokenService.createRefreshToken(user.getId(), session.getId());
        session.setAccessToken(accessToken);
        session.setRefreshToken(refreshToken);
        sessionMapper.updateById(session);
        user.setLastLoginTime(now);
        user.setLastLoginIp(loginIp);
        userMapper.updateById(user);
        return new TokenPair(accessToken, refreshToken);
    }

    public TokenPair refresh(String refreshToken) {
        Claims claims = jwtTokenService.parse(refreshToken);
        if (!"refresh".equals(claims.get("type", String.class))) throw new InvalidCredentialsException();
        Long sessionId = claims.get("sid", Long.class);
        SysUserSession session = sessionMapper.selectById(sessionId);
        if (session == null || !ACTIVE.equals(session.getStatus()) || !refreshToken.equals(session.getRefreshToken())
                || session.getRefreshExpireTime().isBefore(LocalDateTime.now())) throw new InvalidCredentialsException();
        String accessToken = jwtTokenService.createAccessToken(session.getUserId(), session.getId());
        String nextRefreshToken = jwtTokenService.createRefreshToken(session.getUserId(), session.getId());
        LocalDateTime now = LocalDateTime.now();
        session.setAccessToken(accessToken);
        session.setRefreshToken(nextRefreshToken);
        session.setExpireTime(now.plusSeconds(jwtTokenService.accessTokenTtlSeconds()));
        session.setRefreshExpireTime(now.plusSeconds(jwtTokenService.refreshTokenTtlSeconds()));
        sessionMapper.updateById(session);
        return new TokenPair(accessToken, nextRefreshToken);
    }

    public AuthenticatedUser authenticate(String accessToken) {
        Claims claims = jwtTokenService.parse(accessToken);
        if (!"access".equals(claims.get("type", String.class))) throw new InvalidCredentialsException();
        Long sessionId = claims.get("sid", Long.class);
        SysUserSession session = sessionMapper.selectById(sessionId);
        if (session == null || !ACTIVE.equals(session.getStatus()) || !accessToken.equals(session.getAccessToken())
                || session.getExpireTime().isBefore(LocalDateTime.now())) throw new InvalidCredentialsException();
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

    public record TokenPair(String token, String refreshToken) { }
    public record AuthenticatedUser(SysUser user, List<String> roles, List<String> authorities) { }
    public record UserInfo(List<String> buttons, List<String> roles, Long userId, String userName, String email, String avatar) { }
    public static class InvalidCredentialsException extends RuntimeException { }
}
