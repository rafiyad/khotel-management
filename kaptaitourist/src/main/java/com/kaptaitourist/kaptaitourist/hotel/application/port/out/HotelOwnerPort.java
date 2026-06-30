package com.kaptaitourist.kaptaitourist.hotel.application.port.out;

import reactor.core.publisher.Mono;

public interface HotelOwnerPort {
    /** Idempotently records that the user owns the hotel. */
    Mono<Void> assignOwner(String userId, String hotelId);
}
