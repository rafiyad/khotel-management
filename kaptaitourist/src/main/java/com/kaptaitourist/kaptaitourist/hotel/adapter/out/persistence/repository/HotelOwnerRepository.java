package com.kaptaitourist.kaptaitourist.hotel.adapter.out.persistence.repository;

import com.kaptaitourist.kaptaitourist.hotel.adapter.out.persistence.entity.HotelOwnerEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface HotelOwnerRepository extends R2dbcRepository<HotelOwnerEntity, String> {

    @Query("SELECT EXISTS(SELECT 1 FROM khotel_hotel_owner WHERE user_id = :userId AND hotel_id = :hotelId)")
    Mono<Boolean> existsByUserIdAndHotelId(String userId, String hotelId);

    @Query("SELECT * FROM khotel_hotel_owner WHERE user_id = :userId AND hotel_id = :hotelId")
    Mono<HotelOwnerEntity> findByUserIdAndHotelId(String userId, String hotelId);
}
