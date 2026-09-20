package com.xqy.sms.common.security.rpc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

/** HMAC protection for service-to-service calls when mTLS protects the transport. */
public final class InternalCallSigner {
    private static final String HMAC = "HmacSHA256";
    private final byte[] secret;
    private final Clock clock;
    private final Duration maxAge;

    public InternalCallSigner(String secret) {
        this(secret, Clock.systemUTC(), Duration.ofMinutes(5));
    }

    InternalCallSigner(String secret, Clock clock, Duration maxAge) {
        if (secret == null || secret.trim().length() < 32) {
            throw new IllegalArgumentException("sms.internal-rpc.secret must be at least 32 characters");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.clock = Objects.requireNonNull(clock, "clock");
        this.maxAge = Objects.requireNonNull(maxAge, "maxAge");
    }

    public InternalCallContext sign(String callerService, String tenantId, Long userId, Long sessionId,
                                    String requestId) {
        long issuedAt = clock.instant().getEpochSecond();
        String payload = payload(callerService, tenantId, userId, sessionId, requestId, issuedAt);
        return new InternalCallContext(callerService, tenantId, userId, sessionId, requestId, issuedAt, sign(payload));
    }

    public void verify(InternalCallContext context, String expectedCaller) {
        if (context == null || !expectedCaller.equals(context.callerService()) || blank(context.tenantId())
                || context.userId() == null || context.sessionId() == null || blank(context.requestId())
                || blank(context.signature())) {
            throw new InternalCallAuthenticationException();
        }
        long now = clock.instant().getEpochSecond();
        if (context.issuedAtEpochSecond() > now + 30 || now - context.issuedAtEpochSecond() > maxAge.toSeconds()) {
            throw new InternalCallAuthenticationException();
        }
        byte[] actual = sign(payload(context.callerService(), context.tenantId(), context.userId(),
                context.sessionId(), context.requestId(), context.issuedAtEpochSecond())).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(actual, context.signature().getBytes(StandardCharsets.UTF_8))) {
            throw new InternalCallAuthenticationException();
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(secret, HMAC));
            return java.util.HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign internal RPC context", exception);
        }
    }

    private String payload(String caller, String tenant, Long user, Long session, String requestId, long issuedAt) {
        return String.join("|", nullToEmpty(caller), nullToEmpty(tenant), String.valueOf(user),
                String.valueOf(session), nullToEmpty(requestId), String.valueOf(issuedAt));
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String nullToEmpty(String value) { return value == null ? "" : value; }

    public static class InternalCallAuthenticationException extends RuntimeException { }
}
