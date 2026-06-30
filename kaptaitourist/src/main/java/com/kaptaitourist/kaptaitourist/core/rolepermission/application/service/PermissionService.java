package com.kaptaitourist.kaptaitourist.core.rolepermission.application.service;


import com.kaptaitourist.kaptaitourist.core.rolepermission.application.port.in.PermissionUseCase;
import com.kaptaitourist.kaptaitourist.core.rolepermission.application.port.out.PermissionPersistencePort;
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

    public Mono<Boolean> hasPermission(List<String> roleNames, String requestUrl, String httpMethod) {
        if (requestUrl == null || requestUrl.isBlank()) {
            log.error("Permission check failed: requestUrl is null or blank");
            return Mono.just(false);
        }
        if (httpMethod == null || httpMethod.isBlank()) {
            log.error("Permission check failed: httpMethod is null or blank");
            return Mono.just(false);
        }

        // A permission row with permission_name = 'ALL' marks the endpoint as public:
        // any caller (any/no role) is allowed if the URL matches.
        return persistencePort
                .getPublicPermissionList(httpMethod)
                .filter(permission -> pathMatcher.match(permission.getUrl(), requestUrl))
                .hasElements()
                .flatMap(isPublic -> {
                    if (isPublic) {
                        log.info("Public (ALL) permission matched for {} {}", httpMethod, requestUrl);
                        return Mono.just(true);
                    }
                    if (roleNames == null || roleNames.isEmpty()) {
                        log.error("Permission check failed: roleNames is null or empty");
                        return Mono.just(false);
                    }
                    return persistencePort
                            .getAllPermissionList(roleNames, httpMethod)
                            .doOnNext(permissions -> log.info("Permissions found with roles {} and method {}: {}", roleNames, httpMethod, permissions))
                            .filter(permission -> pathMatcher.match(permission.getUrl(), requestUrl))
                            .doOnNext(permission -> log.info("Matched Permissions: {}", permission.toString()))
                            .hasElements();
                })
                .onErrorResume(throwable -> Mono.just(false));
    }

}
