package com.kaptaitourist.kaptaitourist.hotel.application.port.in;

import com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto.HotelListResponseDto;
import com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto.HotelRequestDto;
import com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto.HotelResponseDto;
import reactor.core.publisher.Mono;

public interface HotelUseCase {

    Mono<HotelResponseDto> createHotel(HotelRequestDto dto);

    Mono<HotelListResponseDto> findAll();

    Mono<HotelResponseDto> findById(String id);

    Mono<HotelResponseDto> updateHotel(String id, HotelRequestDto dto);

    Mono<Void> deleteHotel(String id);
}
