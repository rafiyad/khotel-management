package com.kaptaitourist.kaptaitourist.hotel.application.service;

import com.kaptaitourist.kaptaitourist.core.exception.HotelNotFoundException;
import com.kaptaitourist.kaptaitourist.core.exception.ValidationException;
import com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto.HotelListResponseDto;
import com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto.HotelRequestDto;
import com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto.HotelResponseDto;
import com.kaptaitourist.kaptaitourist.hotel.application.port.in.HotelUseCase;
import com.kaptaitourist.kaptaitourist.hotel.application.port.out.HotelOwnerPort;
import com.kaptaitourist.kaptaitourist.hotel.application.port.out.HotelPort;
import com.kaptaitourist.kaptaitourist.hotel.domain.Hotel;
import com.kaptaitourist.kaptaitourist.image.application.port.in.ImageUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotelService implements HotelUseCase {

    private final HotelPort hotelPort;
    private final ImageUseCase imageUseCase;
    private final HotelOwnerPort hotelOwnerPort;

    // ----------------------------------- Create -------------------------------------------

    @Override
    public Mono<HotelResponseDto> createHotel(HotelRequestDto dto, String creatorUserId) {
        if (dto.getName() == null || dto.getName().isBlank())
            return Mono.error(new ValidationException("Hotel name is required"));

        Hotel hotel = Hotel.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .checkInTime(dto.getCheckInTime())
                .checkOutTime(dto.getCheckOutTime())
                .mobile(dto.getMobile())
                .email(dto.getEmail())
                .website(dto.getWebsite())
                .address(dto.getAddress())
                .googleMapUrl(dto.getGoogleMapUrl())
                .createdBy(creatorUserId)
                .createdAt(LocalDateTime.now())
                .build();

        // Persist the hotel, then record the creator as an owner so they (and only they,
        // plus admins) can manage it.
        return hotelPort.save(hotel)
                .flatMap(saved -> hotelOwnerPort.assignOwner(creatorUserId, saved.getId()).thenReturn(saved))
                .map(saved -> HotelResponseDto.builder()
                        .message("Hotel created successfully")
                        .hotelData(saved)
                        .build())
                .doOnSuccess(r -> log.info("Created hotel id: {} owned by {}", r.getHotelData().getId(), creatorUserId))
                .doOnError(e -> log.error("Error creating hotel: {}", e.getMessage()));
    }

    // ----------------------------------- Find all -----------------------------------------

    @Override
    public Mono<HotelListResponseDto> findAll() {
        return hotelPort.findAll()
                .collectList()
                .map(list -> HotelListResponseDto.builder()
                        .message("Hotels retrieved successfully")
                        .totalRecords(list.size())
                        .hotelData(list)
                        .build())
                .doOnError(e -> log.error("Error finding hotels: {}", e.getMessage()));
    }

    // ----------------------------------- Find by id ---------------------------------------

    @Override
    public Mono<HotelResponseDto> findById(String id) {
        return hotelPort.findById(id)
                .switchIfEmpty(Mono.error(new HotelNotFoundException("Hotel not found with id: " + id)))
                .map(hotel -> HotelResponseDto.builder()
                        .message("Hotel retrieved successfully")
                        .hotelData(hotel)
                        .build())
                .doOnError(e -> log.error("Error finding hotel id {}: {}", id, e.getMessage()));
    }

    // ----------------------------------- Update -------------------------------------------

    @Override
    public Mono<HotelResponseDto> updateHotel(String id, HotelRequestDto dto) {
        if (dto.getName() == null || dto.getName().isBlank())
            return Mono.error(new ValidationException("Hotel name is required"));

        return hotelPort.findById(id)
                .switchIfEmpty(Mono.error(new HotelNotFoundException("Hotel not found with id: " + id)))
                .flatMap(existing -> {
                    // Full replace (PUT), preserving identity, version and creation audit.
                    Hotel updated = Hotel.builder()
                            .id(existing.getId())
                            .name(dto.getName())
                            .description(dto.getDescription())
                            .checkInTime(dto.getCheckInTime())
                            .checkOutTime(dto.getCheckOutTime())
                            .mobile(dto.getMobile())
                            .email(dto.getEmail())
                            .website(dto.getWebsite())
                            .address(dto.getAddress())
                            .googleMapUrl(dto.getGoogleMapUrl())
                            .version(existing.getVersion())
                            .createdBy(existing.getCreatedBy())
                            .createdAt(existing.getCreatedAt())
                            .updatedBy(dto.getUpdatedBy())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return hotelPort.save(updated);
                })
                .map(saved -> HotelResponseDto.builder()
                        .message("Hotel updated successfully")
                        .hotelData(saved)
                        .build())
                .doOnError(e -> log.error("Error updating hotel id {}: {}", id, e.getMessage()));
    }

    // ----------------------------------- Delete -------------------------------------------

    @Override
    public Mono<Void> deleteHotel(String id) {
        return hotelPort.existsById(id)
                .flatMap(exists -> exists
                        // Every image of the hotel (both hotel-level and its rooms' images) carries
                        // this hotel_id, so deleteAllByHotelId removes all storage objects + rows.
                        // Done BEFORE the hotel delete so the FK cascade doesn't orphan storage files.
                        ? imageUseCase.deleteAllByHotelId(id).then(hotelPort.deleteById(id))
                        : Mono.error(new HotelNotFoundException("Hotel not found with id: " + id)))
                .doOnSuccess(v -> log.info("Deleted hotel id: {}", id))
                .doOnError(e -> log.error("Error deleting hotel id {}: {}", id, e.getMessage()));
    }
}
