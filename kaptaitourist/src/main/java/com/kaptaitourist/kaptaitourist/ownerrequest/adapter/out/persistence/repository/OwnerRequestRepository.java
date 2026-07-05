package com.kaptaitourist.kaptaitourist.ownerrequest.adapter.out.persistence.repository;

import com.kaptaitourist.kaptaitourist.ownerrequest.adapter.out.persistence.entity.OwnerRequestEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface OwnerRequestRepository extends R2dbcRepository<OwnerRequestEntity, String> {

    @Query("SELECT * FROM khotel_owner_request WHERE id = :id")
    Mono<OwnerRequestEntity> findById(String id);
}
