package com.xqy.sms.common.security.jwt;

import org.junit.jupiter.api.Test;

import java.util.List;
import com.auth0.jwt.JWT;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtTokenServiceTest {

    @Test
    void createsAndVerifiesSignedContextClaims() {
        JwtTokenService service = new JwtTokenService(
                "test-signing-secret-must-be-at-least-thirty-two-bytes", 60, 120);
        JwtUserContext source = new JwtUserContext(10L, 20L, "source-token-id", "tenant-1", List.of("ADMIN"),
                List.of("student:read"), List.of("CLASS:100"), JwtTokenService.ACCESS_TOKEN);

        JwtUserContext access = service.verifyAccessToken(service.createAccessToken(source));
        String refreshToken = service.createRefreshToken(source);
        JwtUserContext refresh = service.verify(refreshToken);

        assertEquals(10L, access.userId());
        assertEquals(20L, access.sessionId());
        org.junit.jupiter.api.Assertions.assertNotNull(access.tokenId());
        assertEquals("tenant-1", access.tenantId());
        assertEquals(List.of("ADMIN"), access.roles());
        assertEquals(List.of("student:read"), access.authorities());
        assertEquals(List.of("CLASS:100"), access.dataScopes());
        assertEquals(JwtTokenService.REFRESH_TOKEN, refresh.tokenType());
        var decoded = JWT.decode(refreshToken);
        assertEquals("10", decoded.getSubject());
        assertEquals(20L, decoded.getClaim("sid").asLong());
        org.junit.jupiter.api.Assertions.assertFalse(decoded.getId().isBlank());
        assertEquals("refresh", decoded.getClaim("typ").asString());
        org.junit.jupiter.api.Assertions.assertNotNull(decoded.getExpiresAt());
    }
}
