package com.xqy.sms.system.provider.config;

import com.xqy.sms.common.security.jwt.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfiguration {
    @Bean
    JwtTokenService jwtTokenService(@Value("${sms.system.jwt.secret}") String secret,
                                    @Value("${sms.system.jwt.access-token-ttl-seconds}") long accessTtl,
                                    @Value("${sms.system.jwt.refresh-token-ttl-seconds}") long refreshTtl) {
        return new JwtTokenService(secret, accessTtl, refreshTtl);
    }
}
