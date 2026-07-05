package com.kaptaitourist.kaptaitourist.image.adapter.out.persistence.repository;

import com.kaptaitourist.kaptaitourist.image.adapter.out.persistence.entity.ImageEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface ImageRepository extends R2dbcRepository<ImageEntity, String> {

    @Query("SELECT * FROM khotel_attachment WHERE id = :id")
    Mono<ImageEntity> findById(String id);

    @Query("SELECT * FROM khotel_attachment WHERE hotel_id = :hotelId ORDER BY display_order ASC")
    Flux<ImageEntity> findAllByHotelId(String hotelId);

    @Query("SELECT * FROM khotel_attachment WHERE hotel_id = :hotelId AND room_id IS NULL ORDER BY display_order ASC")
    Flux<ImageEntity> findAllByHotelIdAndRoomIdIsNull(String hotelId);

    @Query("SELECT * FROM khotel_attachment WHERE room_id = :roomId ORDER BY display_order ASC")
    Flux<ImageEntity> findAllByRoomId(String roomId);

    @Query("SELECT * FROM khotel_attachment WHERE room_id IN (:roomIds) ORDER BY display_order ASC")
    Flux<ImageEntity> findAllByRoomIdIn(Collection<String> roomIds);

    @Query("SELECT * FROM khotel_attachment WHERE id = :id AND hotel_id = :hotelId")
    Mono<ImageEntity> findByIdAndHotelId(String id, String hotelId);

    @Modifying
    @Query("DELETE FROM khotel_attachment WHERE id = :id AND hotel_id = :hotelId")
    Mono<Void> DeleteByIdAndHotelId(String id, String hotelId);

    @Modifying
    @Query("DELETE FROM khotel_attachment WHERE hotel_id = :hotelId")
    Mono<Void> DeleteAllByHotelId(String hotelId);

    @Query("SELECT * FROM khotel_attachment WHERE id = :id AND room_id = :roomId")
    Mono<ImageEntity> findByIdAndRoomId(String id, String roomId);

    @Modifying
    @Query("DELETE FROM khotel_attachment WHERE id = :id AND room_id = :roomId")
    Mono<Void> deleteByIdAndRoomId(String id, String roomId);

    @Modifying
    @Query("DELETE FROM khotel_attachment WHERE room_id = :roomId")
    Mono<Void> deleteAllByRoomId(String roomId);

    // ---- Set-primary (unset existing primary in the same gallery, then mark the target) ----

    @Modifying
    @Query("UPDATE khotel_attachment SET is_primary = false WHERE hotel_id = :hotelId AND room_id IS NULL AND is_primary = true")
    Mono<Long> clearHotelPrimary(String hotelId);

    @Modifying
    @Query("UPDATE khotel_attachment SET is_primary = false WHERE room_id = :roomId AND is_primary = true")
    Mono<Long> clearRoomPrimary(String roomId);

    @Modifying
    @Query("UPDATE khotel_attachment SET is_primary = true WHERE id = :id")
    Mono<Long> markPrimary(String id);
}