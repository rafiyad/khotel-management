package com.kaptaitourist.kaptaitourist.image.application.port.out;

import com.kaptaitourist.kaptaitourist.image.domain.Image;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface ImagePort {
    Mono<Image> save(Image image);
    Flux<Image> findAllByHotelId(String hotelId);
    Flux<Image> findAllByRoomId(String roomId);
    Flux<Image> findAllByRoomIdIn(Collection<String> roomIds);
    Mono<Image> findByIdAndHotelId(String id, String hotelId);
    Mono<Void> DeleteByIdAndHotelId(String id, String hotelId);
    Mono<Void> DeleteAllByHotelId(String hotelId);

    Mono<Image> findByIdAndRoomId(String id, String roomId);
    Mono<Void> deleteByIdAndRoomId(String id, String roomId);
    Mono<Void> deleteAllByRoomId(String roomId);
}
