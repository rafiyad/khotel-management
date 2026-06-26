package com.kaptaitourist.kaptaitourist.hotel.adapter.out.persistence.repository;

import com.kaptaitourist.kaptaitourist.hotel.adapter.out.persistence.entity.HotelEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface HotelRepository extends R2dbcRepository<HotelEntity, String> {
}
