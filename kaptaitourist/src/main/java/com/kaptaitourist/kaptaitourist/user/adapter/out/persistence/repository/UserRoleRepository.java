package com.kaptaitourist.kaptaitourist.user.adapter.out.persistence.repository;

import com.kaptaitourist.kaptaitourist.user.adapter.out.persistence.entity.UserRoleEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRoleRepository extends R2dbcRepository<UserRoleEntity, String> {

    @Query("SELECT * FROM khotel_user_role WHERE user_id = :userId AND role_id = :roleId")
    Mono<UserRoleEntity> findByUserIdAndRoleId(String userId, String roleId);

    @Query("""
            SELECT r.name FROM khotel_role r
            JOIN khotel_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = :userId
            ORDER BY r.name
            """)
    Flux<String> findRoleNamesByUserId(String userId);
}
