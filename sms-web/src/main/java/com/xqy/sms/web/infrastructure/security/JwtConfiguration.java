package com.xqy.sms.web.infrastructure.security;

import com.xqy.sms.common.security.jwt.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfiguration {

    @Bean
    public JwtTokenService jwtTokenService(@Value("${sms.web.jwt.secret}") String secret,
                                           @Value("${sms.web.jwt.access-token-ttl-seconds}") long accessTokenTtlSeconds,
                                           @Value("${sms.web.jwt.refresh-token-ttl-seconds}") long refreshTokenTtlSeconds) {
        return new JwtTokenService(secret, accessTokenTtlSeconds, refreshTokenTtlSeconds);
    }
}
