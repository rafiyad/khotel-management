package com.kaptaitourist.kaptaitourist.booking.application.port.out;

import com.kaptaitourist.kaptaitourist.booking.domain.Booking;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface BookingPort {
    Mono<Booking> save(Booking booking);
    Flux<Booking> findAllByHotelId(String hotelId);
    Mono<Booking> findByIdAndHotelId(String bookingId, String hotelId);

    /** Locks the room row and returns its total_units (use inside a transaction). */
    Mono<Integer> lockRoomTotalUnits(String roomId);

    /** Units booked (non-cancelled) for a room type overlapping [checkIn, checkOut). */
    Mono<Long> sumBookedUnits(String roomId, LocalDate checkIn, LocalDate checkOut);
}
