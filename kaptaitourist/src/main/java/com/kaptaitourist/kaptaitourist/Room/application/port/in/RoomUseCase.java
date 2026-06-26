package com.kaptaitourist.kaptaitourist.Room.application.port.in;

import com.kaptaitourist.kaptaitourist.Room.adapter.in.web.dto.RoomListResponseDto;
import com.kaptaitourist.kaptaitourist.Room.adapter.in.web.dto.RoomRequestDto;
import com.kaptaitourist.kaptaitourist.Room.adapter.in.web.dto.RoomResponseDto;
import reactor.core.publisher.Mono;

public interface RoomUseCase {

    Mono<RoomResponseDto> createRoom(String hotelId, RoomRequestDto dto);

    Mono<RoomListResponseDto> findAllByHotelId(String hotelId);

    Mono<RoomResponseDto> findById(String hotelId, String roomId);

    Mono<RoomResponseDto> updateRoom(String hotelId, String roomId, RoomRequestDto dto);

    Mono<Void> deleteRoom(String hotelId, String roomId);
}
