package com.kaptaitourist.kaptaitourist.facility.adapter.out.persistence.repository;

import com.kaptaitourist.kaptaitourist.facility.adapter.out.persistence.entity.FacilityEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface FacilityRepository extends R2dbcRepository<FacilityEntity, String> {

    @Query("SELECT * FROM khotel_facility ORDER BY name ASC")
    Flux<FacilityEntity> findAllOrdered();
}
