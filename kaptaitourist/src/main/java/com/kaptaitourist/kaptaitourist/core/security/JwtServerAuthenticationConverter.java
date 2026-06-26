package com.kaptaitourist.kaptaitourist.core.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Extracts a Bearer token from the Authorization header into an unauthenticated
 * Authentication carrying the raw token (validated later by the auth manager).
 */
public class JwtServerAuthenticationConverter implements ServerAuthenticationConverter {

    private static final String BEARER = "Bearer ";

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            return Mono.empty();
        }
        String token = header.substring(BEARER.length()).trim();
        if (token.isEmpty()) {
            return Mono.empty();
        }
        // principal=token, credentials=token; 2-arg ctor marks it unauthenticated.
        return Mono.just(new UsernamePasswordAuthenticationToken(token, token));
    }
}
