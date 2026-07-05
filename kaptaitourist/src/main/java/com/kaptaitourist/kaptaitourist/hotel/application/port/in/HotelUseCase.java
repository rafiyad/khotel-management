package com.kaptaitourist.kaptaitourist.hotel.application.port.in;

import com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto.HotelListResponseDto;
import com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto.HotelRequestDto;
import com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto.AdminHotelListResponseDto;
import com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto.HotelResponseDto;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

public interface HotelUseCase {

    Mono<HotelResponseDto> createHotel(HotelRequestDto dto, String creatorUserId);

    /** Hotels owned by the caller (enriched with rooms/images/facilities). */
    Mono<HotelListResponseDto> findOwnerHotels(String userId);

    /** Admin oversight list: all hotels + owners + counts, no image enrichment. */
    Mono<AdminHotelListResponseDto> findAdminHotels();

    /**
     * Lists hotels, paginated, with optional filters (all AND together): {@code search} (name),
     * {@code facilityIds} (ALL-match at hotel level), and a date window {@code checkIn}/{@code checkOut}
     * with {@code guests} that keeps only hotels having ≥1 room free for those dates.
     */
    Mono<HotelListResponseDto> findAll(String search, List<String> facilityIds,
                                       LocalDate checkIn, LocalDate checkOut, Integer guests,
                                       int page, int size);

    Mono<HotelResponseDto> findById(String id);

    Mono<HotelResponseDto> updateHotel(String id, HotelRequestDto dto, String updaterUserId);

    Mono<Void> deleteHotel(String id);
}
