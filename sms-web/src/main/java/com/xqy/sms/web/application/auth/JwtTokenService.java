package com.xqy.sms.web.application.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtTokenService {
    private final SecretKey signingKey;
    private final long accessTokenTtlSeconds;
    private final long refreshTokenTtlSeconds;

    public JwtTokenService(@Value("${sms.web.jwt.secret}") String secret,
                           @Value("${sms.web.jwt.access-token-ttl-seconds}") long accessTokenTtlSeconds,
                           @Value("${sms.web.jwt.refresh-token-ttl-seconds}") long refreshTokenTtlSeconds) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("sms.web.jwt.secret must contain at least 32 bytes");
        }
        if (accessTokenTtlSeconds <= 0 || refreshTokenTtlSeconds <= 0) {
            throw new IllegalArgumentException("JWT expiration values must be positive");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    public String createAccessToken(Long userId, Long sessionId) {
        return createToken(userId, sessionId, "access", accessTokenTtlSeconds);
    }

    public String createRefreshToken(Long userId, Long sessionId) {
        return createToken(userId, sessionId, "refresh", refreshTokenTtlSeconds);
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    public long accessTokenTtlSeconds() { return accessTokenTtlSeconds; }
    public long refreshTokenTtlSeconds() { return refreshTokenTtlSeconds; }

    private String createToken(Long userId, Long sessionId, String type, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("sid", sessionId)
                .claim("type", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(signingKey)
                .compact();
    }
}
