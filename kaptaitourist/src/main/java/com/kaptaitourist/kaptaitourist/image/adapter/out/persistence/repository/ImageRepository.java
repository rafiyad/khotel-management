package com.kaptaitourist.kaptaitourist.image.adapter.out.persistence.repository;

import com.kaptaitourist.kaptaitourist.image.adapter.out.persistence.entity.ImageEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface ImageRepository extends R2dbcRepository<ImageEntity, String> {

}
