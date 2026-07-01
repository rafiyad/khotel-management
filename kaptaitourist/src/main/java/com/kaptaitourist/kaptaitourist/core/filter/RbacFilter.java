package com.kaptaitourist.kaptaitourist.core.filter;

import com.kaptaitourist.kaptaitourist.core.rolepermission.application.port.in.PermissionUseCase;
import com.kaptaitourist.kaptaitourist.core.security.SecurityErrorWriter;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;


@Order(Ordered.LOWEST_PRECEDENCE)   // run after the Spring Security chain so the SecurityContext is populated
@AllArgsConstructor
@Component
@Slf4j
public class RbacFilter implements WebFilter {

    private final PermissionUseCase permissionUseCase;

    @Override
    public @NonNull Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        log.info("RBAC filter path to be authorized: {}", path);

        String method = exchange.getRequest().getMethod().name();
        log.info("RBAC filter method to be authorized: {}", method);

        // CORS preflight must always be allowed through
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // Resolve whether an authenticated principal is present BEFORE invoking the chain.
        // authorize(...) ends in chain.filter(exchange), a Mono<Void> that completes empty on
        // success — so we must not use switchIfEmpty to model "no authentication", otherwise a
        // successfully handled authenticated request (also empty) would re-trigger the filter and
        // attempt to write to an already-committed response. Materialising the presence/absence of
        // authentication into an Optional guarantees authorize(...) runs exactly once.
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication != null && authentication.isAuthenticated())
                .doOnNext(authentication -> log.info("RBAC filter authentication: {}", authentication))
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(optionalAuth -> authorize(exchange, chain, optionalAuth.orElse(null),
                        path, method, optionalAuth.isEmpty()));
    }

    private Mono<Void> authorize(ServerWebExchange exchange, WebFilterChain chain, Authentication authentication,
                                 String path, String method, boolean unauthenticated) {
        List<String> roles = authentication == null ? List.of()
                : authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)   // plain role names, e.g. "ADMIN"
                        .toList();
        log.info("RBAC filter authorities: {}", roles);

        return permissionUseCase.hasPermission(roles, path, method)
                .flatMap(hasPermission -> {
                    if (hasPermission) {
                        return chain.filter(exchange);
                    }
                    if (unauthenticated) {
                        log.info("RBAC filter authentication required for path: {} and method: {}", path, method);
                        return SecurityErrorWriter.write(exchange, HttpStatus.UNAUTHORIZED,
                                "Authentication required. Please provide a valid token to access this resource.");
                    }
                    log.info("RBAC filter access denied for path: {} and method: {}", path, method);
                    return SecurityErrorWriter.write(exchange, HttpStatus.FORBIDDEN,
                            "Access Denied. Restricted. You are not allowed to access this resource.");
                });
    }
}
