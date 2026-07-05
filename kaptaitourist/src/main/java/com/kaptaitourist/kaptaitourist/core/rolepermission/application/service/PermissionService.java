package com.kaptaitourist.kaptaitourist.core.rolepermission.application.service;


import com.kaptaitourist.kaptaitourist.core.rolepermission.application.port.in.PermissionUseCase;
import com.kaptaitourist.kaptaitourist.core.rolepermission.application.port.out.PermissionPersistencePort;
import com.kaptaitourist.kaptaitourist.core.rolepermission.domain.AuthorizationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService implements PermissionUseCase {

    private final PermissionPersistencePort persistencePort;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<AuthorizationResult> authorize(List<String> roleNames, String requestUrl, String httpMethod) {
        if (requestUrl == null || requestUrl.isBlank() || httpMethod == null || httpMethod.isBlank()) {
            log.error("Authorization failed: blank url/method (url={}, method={})", requestUrl, httpMethod);
            return Mono.just(AuthorizationResult.denied());
        }

        // A permission row with permission_name = 'ALL' marks the endpoint as public (never
        // ownership-scoped): any caller — with any role or none — is allowed when the URL matches.
        return persistencePort
                .getPublicPermissionList(httpMethod)
                .filter(permission -> pathMatcher.match(permission.getUrl(), requestUrl))
                .hasElements()
                .flatMap(isPublic -> {
                    if (isPublic) {
                        log.info("Public (ALL) permission matched for {} {}", httpMethod, requestUrl);
                        return Mono.just(AuthorizationResult.granted(false, null));
                    }
                    if (roleNames == null || roleNames.isEmpty()) {
                        return Mono.just(AuthorizationResult.denied());
                    }
                    // First permission whose URL template matches; carry its ownership requirement
                    // and template back so the filter can extract {hotelId} and verify ownership.
                    return persistencePort
                            .getAllPermissionList(roleNames, httpMethod)
                            .filter(permission -> pathMatcher.match(permission.getUrl(), requestUrl))
                            .next()
                            .map(permission -> {
                                log.info("Matched permission {} for {} {} (requiresOwnership={})",
                                        permission.getPermissionName(), httpMethod, requestUrl, permission.getRequiresOwnership());
                                return AuthorizationResult.granted(
                                        Boolean.TRUE.equals(permission.getRequiresOwnership()), permission.getUrl());
                            })
                            .defaultIfEmpty(AuthorizationResult.denied());
                })
                // Fail closed, but never silently: a DB outage must be logged, not masked as a 403.
                .onErrorResume(throwable -> {
                    log.error("Authorization error for {} {}: {}", httpMethod, requestUrl, throwable.getMessage(), throwable);
                    return Mono.just(AuthorizationResult.denied());
                });
    }
}
