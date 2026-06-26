package com.kaptaitourist.kaptaitourist.Room.adapter.out.persistence.repository;

import com.kaptaitourist.kaptaitourist.Room.adapter.out.persistence.entity.RoomEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RoomRepository extends R2dbcRepository<RoomEntity, String> {

    @Query("SELECT * FROM khotel_room WHERE hotel_id = :hotelId ORDER BY room_name ASC")
    Flux<RoomEntity> findAllByHotelId(String hotelId);

    @Query("SELECT * FROM khotel_room WHERE id = :roomId AND hotel_id = :hotelId")
    Mono<RoomEntity> findByIdAndHotelId(String roomId, String hotelId);
}
