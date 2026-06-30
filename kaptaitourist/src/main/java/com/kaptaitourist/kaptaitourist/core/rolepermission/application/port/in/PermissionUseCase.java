package com.kaptaitourist.kaptaitourist.core.rolepermission.application.port.in;


import reactor.core.publisher.Mono;

import java.util.List;

public interface PermissionUseCase {
    Mono<Boolean> hasPermission(List<String> roleNames, String requestUrl, String httpMethod);

}
