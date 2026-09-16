package com.xqy.sms.ai.infrastructure.security;

import com.xqy.sms.common.security.jwt.JwtTokenService;
import com.xqy.sms.common.security.jwt.JwtUserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class AiJwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService jwtTokenService;

    public AiJwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = bearerToken(request);
        if (token != null) {
            try {
                JwtUserContext userContext = jwtTokenService.verifyAccessToken(token);
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                userContext.roles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
                userContext.authorities().forEach(authority -> authorities.add(new SimpleGrantedAuthority(authority)));
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userContext, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String bearerToken(HttpServletRequest request) {
        String value = request.getHeader("Authorization");
        if (value == null || !value.regionMatches(true, 0, "Bearer ", 0, 7)) return null;
        String token = value.substring(7).trim();
        return token.isEmpty() ? null : token;
    }
}
