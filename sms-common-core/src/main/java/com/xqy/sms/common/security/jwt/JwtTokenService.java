package com.xqy.sms.common.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/** Framework-neutral JWT signing and verification service shared by application modules. */
public final class JwtTokenService {
    public static final String ACCESS_TOKEN = "access";
    public static final String REFRESH_TOKEN = "refresh";

    private static final String SESSION_ID_CLAIM = "sid";
    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String TENANT_ID_CLAIM = "tenant_id";
    private static final String ROLES_CLAIM = "roles";
    private static final String AUTHORITIES_CLAIM = "authorities";
    private static final String DATA_SCOPES_CLAIM = "data_scopes";

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final long accessTokenTtlSeconds;
    private final long refreshTokenTtlSeconds;

    public JwtTokenService(String secret, long accessTokenTtlSeconds, long refreshTokenTtlSeconds) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 bytes");
        }
        if (accessTokenTtlSeconds <= 0 || refreshTokenTtlSeconds <= 0) {
            throw new IllegalArgumentException("JWT expiration values must be positive");
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).build();
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    public String createAccessToken(JwtUserContext context) {
        return createToken(context, ACCESS_TOKEN, accessTokenTtlSeconds);
    }

    public String createRefreshToken(JwtUserContext context) {
        return createToken(context, REFRESH_TOKEN, refreshTokenTtlSeconds);
    }

    public JwtUserContext verify(String token) {
        DecodedJWT jwt = verifier.verify(token);
        return new JwtUserContext(requiredLong(jwt.getSubject(), "sub"),
                requiredLong(jwt.getClaim(SESSION_ID_CLAIM).asLong(), SESSION_ID_CLAIM),
                jwt.getClaim(TENANT_ID_CLAIM).asString(),
                strings(jwt.getClaim(ROLES_CLAIM).asList(String.class)),
                strings(jwt.getClaim(AUTHORITIES_CLAIM).asList(String.class)),
                strings(jwt.getClaim(DATA_SCOPES_CLAIM).asList(String.class)),
                jwt.getClaim(TOKEN_TYPE_CLAIM).asString());
    }

    public JwtUserContext verifyAccessToken(String token) {
        JwtUserContext context = verify(token);
        if (!ACCESS_TOKEN.equals(context.tokenType())) {
            throw new IllegalArgumentException("JWT is not an access token");
        }
        return context;
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public long refreshTokenTtlSeconds() {
        return refreshTokenTtlSeconds;
    }

    private String createToken(JwtUserContext context, String tokenType, long ttlSeconds) {
        if (context == null) throw new IllegalArgumentException("JWT user context must not be null");
        Instant now = Instant.now();
        return JWT.create()
                .withSubject(String.valueOf(context.userId()))
                .withClaim(SESSION_ID_CLAIM, context.sessionId())
                .withClaim(TOKEN_TYPE_CLAIM, tokenType)
                .withClaim(TENANT_ID_CLAIM, context.tenantId())
                .withArrayClaim(ROLES_CLAIM, context.roles().toArray(String[]::new))
                .withArrayClaim(AUTHORITIES_CLAIM, context.authorities().toArray(String[]::new))
                .withArrayClaim(DATA_SCOPES_CLAIM, context.dataScopes().toArray(String[]::new))
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(ttlSeconds)))
                .sign(algorithm);
    }

    private long requiredLong(String value, String claimName) {
        try {
            if (value == null || value.isBlank()) throw new NumberFormatException();
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("JWT claim " + claimName + " must be a number", exception);
        }
    }

    private long requiredLong(Long value, String claimName) {
        if (value == null) throw new IllegalArgumentException("JWT claim " + claimName + " must be a number");
        return value;
    }

    private List<String> strings(List<String> values) {
        return values == null ? List.of() : values;
    }
}
