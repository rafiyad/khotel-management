package com.kaptaitourist.kaptaitourist.user.adapter.out.persistence.repository;

import com.kaptaitourist.kaptaitourist.user.adapter.out.persistence.entity.RoleEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface RoleRepository extends R2dbcRepository<RoleEntity, String> {

    @Query("SELECT * FROM khotel_role WHERE name = :name")
    Mono<RoleEntity> findByName(String name);
}
