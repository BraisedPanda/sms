package com.xqy.sms.ai.infrastructure.security;

import com.xqy.sms.common.security.jwt.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiJwtConfiguration {

    @Bean
    public JwtTokenService jwtTokenService(@Value("${sms.ai.jwt.secret}") String secret,
                                           @Value("${sms.ai.jwt.access-token-ttl-seconds}") long accessTokenTtlSeconds,
                                           @Value("${sms.ai.jwt.refresh-token-ttl-seconds}") long refreshTokenTtlSeconds) {
        return new JwtTokenService(secret, accessTokenTtlSeconds, refreshTokenTtlSeconds);
    }
}
