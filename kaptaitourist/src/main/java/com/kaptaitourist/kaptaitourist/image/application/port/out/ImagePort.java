package com.kaptaitourist.kaptaitourist.image.application.port.out;

import com.kaptaitourist.kaptaitourist.image.domain.Image;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ImagePort {
    Mono<Image> save(Image image);
    Flux<Image> findAllByHotelId(String hotelId);
    Mono<Image> findByIdAndHotelId(String id, String hotelId);
    Mono<Void> DeleteByIdAndHotelId(String id, String hotelId);
    Mono<Void> DeleteAllByHotelId(String hotelId);
}
