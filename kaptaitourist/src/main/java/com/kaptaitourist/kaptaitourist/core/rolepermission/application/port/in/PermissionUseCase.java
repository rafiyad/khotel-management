package com.kaptaitourist.kaptaitourist.core.rolepermission.application.port.in;


import com.kaptaitourist.kaptaitourist.core.rolepermission.domain.AuthorizationResult;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PermissionUseCase {
    /**
     * Decides whether the given roles may call {@code method requestUrl}. The result also reports
     * whether the matched permission requires resource ownership (checked separately by the filter).
     */
    Mono<AuthorizationResult> authorize(List<String> roleNames, String requestUrl, String httpMethod);
}
