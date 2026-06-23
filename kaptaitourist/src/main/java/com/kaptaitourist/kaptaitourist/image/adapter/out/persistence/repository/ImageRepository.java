package com.kaptaitourist.kaptaitourist.image.adapter.out.persistence.repository;

import com.kaptaitourist.kaptaitourist.image.adapter.out.persistence.entity.ImageEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface ImageRepository extends R2dbcRepository<ImageEntity, String> {
    @Query("SELECT * FROM khotel_attachment WHERE id = :id")
    Mono<ImageEntity> findById(String id);
}
