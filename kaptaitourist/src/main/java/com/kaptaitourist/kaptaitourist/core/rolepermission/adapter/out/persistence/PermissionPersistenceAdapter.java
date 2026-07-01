package com.kaptaitourist.kaptaitourist.core.rolepermission.adapter.out.persistence;


import com.kaptaitourist.kaptaitourist.core.rolepermission.adapter.out.persistence.repository.PermissionRepository;
import com.kaptaitourist.kaptaitourist.core.rolepermission.application.port.out.PermissionPersistencePort;
import com.kaptaitourist.kaptaitourist.core.rolepermission.domain.Permission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionPersistenceAdapter implements PermissionPersistencePort {
    private final PermissionRepository repository;
    private final ModelMapper modelMapper;

    @Override
    public Flux<Permission> getAllPermissionList(List<String> roleNames, String httpMethod) {
        return repository.findPermissionsByRoleNamesAndMethod(roleNames.toArray(new String[0]), httpMethod)
                .map(permissionEntity -> modelMapper.map(permissionEntity, Permission.class))
                .switchIfEmpty(Flux.defer(() -> {
                    log.warn("No permissions found in DB for roles {} and method {}", roleNames, httpMethod);
                    return Flux.empty();
                }))
                .doOnError(e -> log.error("Error occurred fetching data from db: {}", e.getMessage()));
    }

    @Override
    public Flux<Permission> getPublicPermissionList(String httpMethod) {
        return repository.findPublicPermissionsByMethod(httpMethod)
                .map(permissionEntity -> modelMapper.map(permissionEntity, Permission.class))
                .doOnError(e -> log.error("Error occurred fetching public permissions from db: {}", e.getMessage()));
    }
}
