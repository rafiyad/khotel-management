package com.kaptaitourist.kaptaitourist.core.rolepermission.application.port.out;



import com.kaptaitourist.kaptaitourist.core.rolepermission.domain.Permission;
import reactor.core.publisher.Flux;

import java.util.List;

public interface PermissionPersistencePort {
    Flux<Permission> getAllPermissionList(List<String> roleNames, String httpMethod);

    Flux<Permission> getPublicPermissionList(String httpMethod);
}
