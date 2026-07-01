package com.kaptaitourist.kaptaitourist.core.rolepermission.adapter.out.persistence.repository;


import com.kaptaitourist.kaptaitourist.core.rolepermission.adapter.out.persistence.entity.PermissionEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PermissionRepository extends ReactiveCrudRepository<PermissionEntity, String> {

    @Query("""
              SELECT p.*
              FROM permission p
              JOIN role_permission rp ON rp.permission_id = p.id
              JOIN role r ON rp.role_id = r.id
              WHERE r.name = ANY(:roleNames)
                AND p.method = :httpMethod
                AND p.is_deleted = false
                AND r.is_deleted = false
                AND rp.is_deleted = false
            """)
    Flux<PermissionEntity> findPermissionsByRoleNamesAndMethod(@Param("roleNames") String[] roleNames, @Param("httpMethod") String httpMethod);

    @Query("""
              SELECT p.*
              FROM permission p
              WHERE p.permission_name = 'ALL'
                AND p.method = :httpMethod
                AND p.is_deleted = false
            """)
    Flux<PermissionEntity> findPublicPermissionsByMethod(@Param("httpMethod") String httpMethod);


}
