package com.xqy.sms.common.security.jwt;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtTokenServiceTest {

    @Test
    void createsAndVerifiesSignedContextClaims() {
        JwtTokenService service = new JwtTokenService(
                "test-signing-secret-must-be-at-least-thirty-two-bytes", 60, 120);
        JwtUserContext source = new JwtUserContext(10L, 20L, "tenant-1", List.of("ADMIN"),
                List.of("student:read"), List.of("CLASS:100"), JwtTokenService.ACCESS_TOKEN);

        JwtUserContext access = service.verifyAccessToken(service.createAccessToken(source));
        JwtUserContext refresh = service.verify(service.createRefreshToken(source));

        assertEquals(10L, access.userId());
        assertEquals(20L, access.sessionId());
        assertEquals("tenant-1", access.tenantId());
        assertEquals(List.of("ADMIN"), access.roles());
        assertEquals(List.of("student:read"), access.authorities());
        assertEquals(List.of("CLASS:100"), access.dataScopes());
        assertEquals(JwtTokenService.REFRESH_TOKEN, refresh.tokenType());
    }
}
