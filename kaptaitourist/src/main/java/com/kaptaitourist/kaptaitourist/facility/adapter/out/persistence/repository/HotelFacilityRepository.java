package com.kaptaitourist.kaptaitourist.facility.adapter.out.persistence.repository;

import com.kaptaitourist.kaptaitourist.facility.adapter.out.persistence.entity.HotelFacilityEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface HotelFacilityRepository extends R2dbcRepository<HotelFacilityEntity, String> {

    @Query("SELECT * FROM khotel_hotel_facility WHERE hotel_id = :hotelId")
    Flux<HotelFacilityEntity> findAllByHotelId(String hotelId);

    @Query("SELECT * FROM khotel_hotel_facility WHERE hotel_id = :hotelId AND facility_id = :facilityId")
    Mono<HotelFacilityEntity> findByHotelIdAndFacilityId(String hotelId, String facilityId);

    @Modifying
    @Query("DELETE FROM khotel_hotel_facility WHERE hotel_id = :hotelId AND facility_id = :facilityId")
    Mono<Integer> deleteByHotelIdAndFacilityId(String hotelId, String facilityId);
}
