package com.kaptaitourist.kaptaitourist.Room.application.port.out;

import com.kaptaitourist.kaptaitourist.Room.domain.Room;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RoomPort {
    Mono<Room> save(Room room);
    Flux<Room> findAllByHotelId(String hotelId);
    Mono<Room> findByIdAndHotelId(String roomId, String hotelId);
    Mono<Void> deleteById(String id);
}
