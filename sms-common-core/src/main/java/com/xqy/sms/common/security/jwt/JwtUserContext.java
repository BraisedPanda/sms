package com.xqy.sms.common.security.jwt;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Identity and authorization claims carried by a verified JWT. */
public record JwtUserContext(Long userId, Long sessionId, String tenantId, List<String> roles,
                             List<String> authorities, List<String> dataScopes, String tokenType) {

    public JwtUserContext {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        roles = immutableStrings(roles);
        authorities = immutableStrings(authorities);
        dataScopes = immutableStrings(dataScopes);
    }

    private static List<String> immutableStrings(Collection<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }
}
