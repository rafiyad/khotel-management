package com.kaptaitourist.kaptaitourist.core.filter;

import com.kaptaitourist.kaptaitourist.core.rolepermission.application.port.in.PermissionUseCase;
import com.kaptaitourist.kaptaitourist.core.security.OwnershipChecker;
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
import org.springframework.util.AntPathMatcher;
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

    private static final String ROLE_ADMIN = "ADMIN";

    private final PermissionUseCase permissionUseCase;
    private final OwnershipChecker ownershipChecker;
    // Initialized (not constructor-injected) so Lombok's @AllArgsConstructor excludes it.
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public @NonNull Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();
        log.info("RBAC filter authorizing: {} {}", method, path);

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
        String userId = authentication == null ? null : authentication.getName();

        return permissionUseCase.authorize(roles, path, method)
                .flatMap(result -> {
                    if (!result.granted()) {
                        if (unauthenticated) {
                            log.info("RBAC: authentication required for {} {}", method, path);
                            return SecurityErrorWriter.write(exchange, HttpStatus.UNAUTHORIZED,
                                    "Authentication required. Please provide a valid token to access this resource.");
                        }
                        log.info("RBAC: access denied for {} {} (roles={})", method, path, roles);
                        return SecurityErrorWriter.write(exchange, HttpStatus.FORBIDDEN,
                                "Access Denied. Restricted. You are not allowed to access this resource.");
                    }

                    // Role granted. Ownership-scoped endpoints additionally require the caller to
                    // own the hotel in the path — unless they are an ADMIN (who manages everything).
                    if (result.requiresOwnership() && !roles.contains(ROLE_ADMIN)) {
                        String hotelId = extractHotelId(result.urlTemplate(), path);
                        if (hotelId == null) {
                            log.warn("RBAC: ownership required but no {{hotelId}} in template '{}' for path '{}'",
                                    result.urlTemplate(), path);
                            return SecurityErrorWriter.write(exchange, HttpStatus.FORBIDDEN,
                                    "Access Denied. Restricted. You are not allowed to access this resource.");
                        }
                        return ownershipChecker.ownsHotel(userId, hotelId)
                                .flatMap(owns -> {
                                    if (owns) {
                                        return chain.filter(exchange);
                                    }
                                    log.info("RBAC: ownership denied — user {} does not manage hotel {}", userId, hotelId);
                                    return SecurityErrorWriter.write(exchange, HttpStatus.FORBIDDEN,
                                            "Access Denied. You do not manage this hotel.");
                                });
                    }

                    return chain.filter(exchange);
                });
    }

    private String extractHotelId(String urlTemplate, String path) {
        try {
            return pathMatcher.extractUriTemplateVariables(urlTemplate, path).get("hotelId");
        } catch (Exception e) {
            log.warn("Could not extract hotelId from template '{}' / path '{}': {}", urlTemplate, path, e.getMessage());
            return null;
        }
    }
}
