package com.xqy.sms.web.application.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtTokenServiceTest {

    @Test
    void createsSignedAccessAndRefreshTokensWithSessionClaims() {
        JwtTokenService service = new JwtTokenService(
                "test-signing-secret-must-be-at-least-thirty-two-bytes", 60, 120);

        var access = service.parse(service.createAccessToken(10L, 20L));
        var refresh = service.parse(service.createRefreshToken(10L, 20L));

        assertEquals("10", access.getSubject());
        assertEquals(20L, access.get("sid", Long.class));
        assertEquals("access", access.get("type", String.class));
        assertEquals("refresh", refresh.get("type", String.class));
    }
}
