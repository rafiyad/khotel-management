package com.kaptaitourist.kaptaitourist.hotel.application.port.out;

import com.kaptaitourist.kaptaitourist.hotel.domain.Hotel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface HotelPort {
    Mono<Hotel> save(Hotel hotel);
    Flux<Hotel> findAll();
    Mono<Hotel> findById(String id);
    Mono<Void> deleteById(String id);
    Mono<Boolean> existsById(String id);
}
