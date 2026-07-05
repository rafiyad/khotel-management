package com.kaptaitourist.kaptaitourist.hotel.adapter.out.persistence.repository;

import com.kaptaitourist.kaptaitourist.hotel.adapter.out.persistence.entity.HotelEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

// CRUD is inherited (save/findById/findAllById/existsById/deleteById). Filtered, paginated search
// is built dynamically in HotelAdapter via DatabaseClient — the name × facility × date-range filter
// combinations don't fit static derived/@Query methods.
public interface HotelRepository extends R2dbcRepository<HotelEntity, String> {

    @Query("""
            SELECT h.* FROM khotel_hotel h
            JOIN khotel_hotel_owner o ON o.hotel_id = h.id
            WHERE o.user_id = :userId
            ORDER BY h.name
            """)
    Flux<HotelEntity> findByOwnerUserId(String userId);
}
