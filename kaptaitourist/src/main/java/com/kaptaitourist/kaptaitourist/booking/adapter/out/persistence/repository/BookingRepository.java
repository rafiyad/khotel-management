package com.kaptaitourist.kaptaitourist.booking.adapter.out.persistence.repository;

import com.kaptaitourist.kaptaitourist.booking.adapter.out.persistence.entity.BookingEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface BookingRepository extends R2dbcRepository<BookingEntity, String> {

    @Query("SELECT * FROM khotel_booking WHERE hotel_id = :hotelId ORDER BY check_in DESC")
    Flux<BookingEntity> findAllByHotelId(String hotelId);

    @Query("SELECT * FROM khotel_booking WHERE id = :bookingId AND hotel_id = :hotelId")
    Mono<BookingEntity> findByIdAndHotelId(String bookingId, String hotelId);

    /**
     * Locks the room row (FOR UPDATE) and returns its total_units. Must run inside a
     * transaction — the lock serializes concurrent bookings of the same room type so the
     * availability check + insert cannot oversell.
     */
    @Query("SELECT total_units FROM khotel_room WHERE id = :roomId FOR UPDATE")
    Mono<Integer> lockRoomTotalUnits(String roomId);

    /** Units already booked for a room type that overlap [checkIn, checkOut), excluding cancelled. */
    @Query("""
            SELECT COALESCE(SUM(units), 0) FROM khotel_booking
            WHERE room_id = :roomId
              AND status <> 'CANCELLED'
              AND check_in < :checkOut
              AND check_out > :checkIn
            """)
    Mono<Long> sumBookedUnits(String roomId, LocalDate checkIn, LocalDate checkOut);
}
