package com.kaptaitourist.kaptaitourist.facility.adapter.out.persistence.repository;

import com.kaptaitourist.kaptaitourist.facility.adapter.out.persistence.entity.RoomFacilityEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RoomFacilityRepository extends R2dbcRepository<RoomFacilityEntity, String> {

    @Query("SELECT * FROM khotel_room_facility WHERE room_id = :roomId")
    Flux<RoomFacilityEntity> findAllByRoomId(String roomId);

    @Query("SELECT * FROM khotel_room_facility WHERE room_id IN (:roomIds)")
    Flux<RoomFacilityEntity> findAllByRoomIdIn(java.util.Collection<String> roomIds);

    @Query("SELECT * FROM khotel_room_facility WHERE room_id = :roomId AND facility_id = :facilityId")
    Mono<RoomFacilityEntity> findByRoomIdAndFacilityId(String roomId, String facilityId);

    @Modifying
    @Query("DELETE FROM khotel_room_facility WHERE room_id = :roomId AND facility_id = :facilityId")
    Mono<Integer> deleteByRoomIdAndFacilityId(String roomId, String facilityId);
}
