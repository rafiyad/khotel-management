package com.kaptaitourist.kaptaitourist.core.filter;

import com.kaptaitourist.kaptaitourist.core.rolepermission.application.port.in.PermissionUseCase;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;


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

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .doOnNext(authentication -> log.info("RBAC filter authentication: {}", authentication))
                .map(auth -> auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)   // plain role names, e.g. "ADMIN"
                        .toList())
                .doOnNext(authorities -> log.info("RBAC filter authorities: {}", authorities))
                .defaultIfEmpty(List.of())
                .flatMap(roles -> permissionUseCase.hasPermission(roles, path, method))
                .flatMap(hasPermission -> {
                    if(hasPermission){
                        return chain.filter(exchange);
                    } else {
                        log.info("RBAC filter access denied for path: {} and method: {}", path, method);
                        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
                        return exchange.getResponse().setComplete();
                    }
                });
    }
}
