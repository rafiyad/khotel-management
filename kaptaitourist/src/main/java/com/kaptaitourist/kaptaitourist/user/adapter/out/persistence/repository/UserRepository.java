package com.kaptaitourist.kaptaitourist.user.adapter.out.persistence.repository;

import com.kaptaitourist.kaptaitourist.user.adapter.out.persistence.entity.UserEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository extends R2dbcRepository<UserEntity, String> {

    @Query("SELECT * FROM khotel_user WHERE email = :email")
    Mono<UserEntity> findByEmail(String email);

    @Query("SELECT EXISTS(SELECT 1 FROM khotel_user WHERE email = :email)")
    Mono<Boolean> existsByEmail(String email);

    @Query("SELECT EXISTS(SELECT 1 FROM khotel_user WHERE mobile = :mobile)")
    Mono<Boolean> existsByMobile(String mobile);

    @Query("SELECT * FROM khotel_user ORDER BY created_at DESC")
    Flux<UserEntity> findAllOrdered();
}
