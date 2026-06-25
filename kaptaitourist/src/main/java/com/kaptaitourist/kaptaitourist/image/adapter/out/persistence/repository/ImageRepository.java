package com.kaptaitourist.kaptaitourist.image.adapter.out.persistence.repository;

import com.kaptaitourist.kaptaitourist.image.adapter.out.persistence.entity.ImageEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ImageRepository extends R2dbcRepository<ImageEntity, String> {

    @Query("SELECT * FROM khotel_attachment WHERE id = :id")
    Mono<ImageEntity> findById(String id);

    @Query("SELECT * FROM khotel_attachment WHERE hotel_id = :hotelId ORDER BY display_order ASC")
    Flux<ImageEntity> findAllByHotelId(String hotelId);

    @Query("SELECT * FROM khotel_attachment WHERE id = :id AND hotel_id = :hotelId")
    Mono<ImageEntity> findByIdAndHotelId(String id, String hotelId);

    @Modifying
    @Query("DELETE FROM khotel_attachment WHERE id = :id AND hotel_id = :hotelId")
    Mono<Void> DeleteByIdAndHotelId(String id, String hotelId);

    @Modifying
    @Query("DELETE FROM khotel_attachment WHERE hotel_id = :hotelId")
    Mono<Void> DeleteAllByHotelId(String hotelId);
}