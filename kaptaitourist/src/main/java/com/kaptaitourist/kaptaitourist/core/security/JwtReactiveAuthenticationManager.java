package com.kaptaitourist.kaptaitourist.core.security;

import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Validates the raw JWT carried by the unauthenticated token and, on success, returns an
 * authenticated token whose principal is the userId and authorities are ROLE_&lt;name&gt;.
 */
public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtService jwtService;

    public JwtReactiveAuthenticationManager(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = String.valueOf(authentication.getCredentials());
        return Mono.fromCallable(() -> jwtService.parse(token))
                .map(claims -> {
                    String userId = claims.getSubject();
                    List<String> roles = claims.get("roles", List.class);
                    List<SimpleGrantedAuthority> authorities = roles == null ? List.of()
                            : roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
                    Authentication auth = new UsernamePasswordAuthenticationToken(userId, token, authorities);
                    return auth;
                })
                .onErrorResume(e -> Mono.error(new BadCredentialsException("Invalid or expired token")));
    }
}
