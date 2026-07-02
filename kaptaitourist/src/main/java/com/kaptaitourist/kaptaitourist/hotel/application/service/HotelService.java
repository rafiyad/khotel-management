package com.kaptaitourist.kaptaitourist.hotel.application.service;

import com.kaptaitourist.kaptaitourist.core.exception.HotelNotFoundException;
import com.kaptaitourist.kaptaitourist.core.exception.ValidationException;
import com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto.HotelListResponseDto;
import com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto.HotelRequestDto;
import com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto.HotelResponseDto;
import com.kaptaitourist.kaptaitourist.hotel.application.port.in.HotelUseCase;
import com.kaptaitourist.kaptaitourist.Room.application.port.out.RoomPort;
import com.kaptaitourist.kaptaitourist.hotel.application.port.out.HotelOwnerPort;
import com.kaptaitourist.kaptaitourist.hotel.application.port.out.HotelPort;
import com.kaptaitourist.kaptaitourist.hotel.domain.Hotel;
import com.kaptaitourist.kaptaitourist.image.application.port.in.ImageUseCase;
import com.kaptaitourist.kaptaitourist.image.application.port.out.ImagePort;
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
    private final ImagePort imagePort;
    private final RoomPort roomPort;

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
                // R2DBC has no ORM relationships, so images/rooms must be loaded and attached
                // explicitly. flatMapSequential preserves the hotel order while enriching concurrently.
                .flatMapSequential(this::enrich)
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
                .flatMap(this::enrich)
                .map(hotel -> HotelResponseDto.builder()
                        .message("Hotel retrieved successfully")
                        .hotelData(hotel)
                        .build())
                .doOnError(e -> log.error("Error finding hotel id {}: {}", id, e.getMessage()));
    }

    // ----------------------------------- Enrichment ---------------------------------------

    /**
     * Loads and attaches a hotel's related data that R2DBC does not resolve automatically:
     * hotel-level images (room_id IS NULL) and its rooms, with each room carrying its own
     * images. Both image lists arrive already ordered by display_order from the repository.
     */
    private Mono<Hotel> enrich(Hotel hotel) {
        Mono<java.util.List<com.kaptaitourist.kaptaitourist.image.domain.Image>> hotelImages =
                imagePort.findAllByHotelIdAndRoomIdIsNull(hotel.getId()).collectList();

        Mono<java.util.List<com.kaptaitourist.kaptaitourist.Room.domain.Room>> rooms =
                roomPort.findAllByHotelId(hotel.getId())
                        .flatMapSequential(room -> imagePort.findAllByRoomId(room.getId())
                                .collectList()
                                .map(imgs -> {
                                    room.setImages(imgs);
                                    return room;
                                }))
                        .collectList();

        return Mono.zip(hotelImages, rooms)
                .map(tuple -> {
                    hotel.setImages(tuple.getT1());
                    hotel.setRooms(tuple.getT2());
                    return hotel;
                });
    }

    // ----------------------------------- Update -------------------------------------------

    @Override
    public Mono<HotelResponseDto> updateHotel(String id, HotelRequestDto dto, String updaterUserId) {
        // Partial update: only reject an explicitly provided blank name; a null field
        // simply means "leave unchanged".
        if (dto.getName() != null && dto.getName().isBlank())
            return Mono.error(new ValidationException("Hotel name cannot be blank"));

        return hotelPort.findById(id)
                .switchIfEmpty(Mono.error(new HotelNotFoundException("Hotel not found with id: " + id)))
                .flatMap(existing -> {
                    // Merge: keep the existing value for every field the caller left null.
                    // The version is carried over so R2DBC's @Version bumps it (0 -> 1 -> 2 ...)
                    // and enforces optimistic locking on save. updatedBy is the authenticated
                    // user performing the update, never taken from the request body.
                    Hotel updated = Hotel.builder()
                            .id(existing.getId())
                            .name(dto.getName() != null ? dto.getName() : existing.getName())
                            .description(dto.getDescription() != null ? dto.getDescription() : existing.getDescription())
                            .checkInTime(dto.getCheckInTime() != null ? dto.getCheckInTime() : existing.getCheckInTime())
                            .checkOutTime(dto.getCheckOutTime() != null ? dto.getCheckOutTime() : existing.getCheckOutTime())
                            .mobile(dto.getMobile() != null ? dto.getMobile() : existing.getMobile())
                            .email(dto.getEmail() != null ? dto.getEmail() : existing.getEmail())
                            .website(dto.getWebsite() != null ? dto.getWebsite() : existing.getWebsite())
                            .address(dto.getAddress() != null ? dto.getAddress() : existing.getAddress())
                            .googleMapUrl(dto.getGoogleMapUrl() != null ? dto.getGoogleMapUrl() : existing.getGoogleMapUrl())
                            .version(existing.getVersion())
                            .createdBy(existing.getCreatedBy())
                            .createdAt(existing.getCreatedAt())
                            .updatedBy(updaterUserId)
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
