package com.kaptaitourist.kaptaitourist.hotel.application.service;

import com.kaptaitourist.kaptaitourist.core.exception.HotelNotFoundException;
import com.kaptaitourist.kaptaitourist.core.exception.ValidationException;
import com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto.HotelListResponseDto;
import com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto.HotelRequestDto;
import com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto.HotelResponseDto;
import com.kaptaitourist.kaptaitourist.hotel.application.port.in.HotelUseCase;
import com.kaptaitourist.kaptaitourist.Room.application.port.out.RoomPort;
import com.kaptaitourist.kaptaitourist.Room.domain.Room;
import com.kaptaitourist.kaptaitourist.facility.application.port.out.FacilityPort;
import com.kaptaitourist.kaptaitourist.facility.domain.AssignedFacility;
import com.kaptaitourist.kaptaitourist.hotel.application.port.out.HotelOwnerPort;
import com.kaptaitourist.kaptaitourist.hotel.application.port.out.HotelPort;
import com.kaptaitourist.kaptaitourist.hotel.application.port.out.HotelSearchCriteria;
import com.kaptaitourist.kaptaitourist.hotel.domain.Hotel;
import com.kaptaitourist.kaptaitourist.image.application.port.in.ImageUseCase;
import com.kaptaitourist.kaptaitourist.image.application.port.out.ImagePort;
import com.kaptaitourist.kaptaitourist.image.domain.Image;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotelService implements HotelUseCase {

    private final HotelPort hotelPort;
    private final ImageUseCase imageUseCase;
    private final HotelOwnerPort hotelOwnerPort;
    private final ImagePort imagePort;
    private final RoomPort roomPort;
    private final FacilityPort facilityPort;

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

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public Mono<HotelListResponseDto> findAll(String search, List<String> facilityIds,
                                              LocalDate checkIn, LocalDate checkOut, Integer guests,
                                              int page, int size) {
        // Date filter is all-or-nothing: both dates required, checkOut after checkIn, not in the past.
        if ((checkIn == null) != (checkOut == null))
            return Mono.error(new ValidationException("checkIn and checkOut must be provided together"));
        if (checkIn != null) {
            if (!checkOut.isAfter(checkIn))
                return Mono.error(new ValidationException("checkOut must be after checkIn"));
            if (checkIn.isBefore(LocalDate.now()))
                return Mono.error(new ValidationException("checkIn cannot be in the past"));
        }
        if (guests != null && guests < 1)
            return Mono.error(new ValidationException("guests must be at least 1"));

        int safeSize = size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        long offset = (long) safePage * safeSize;
        List<String> facs = facilityIds == null ? List.of()
                : facilityIds.stream().filter(f -> f != null && !f.isBlank()).map(String::trim).distinct().toList();

        HotelSearchCriteria criteria = new HotelSearchCriteria(
                search, facs, checkIn, checkOut, guests == null ? 1 : guests, safeSize, offset);

        Mono<Long> total = hotelPort.count(criteria);
        // R2DBC has no ORM relationships, so images/rooms/facilities are attached explicitly per row.
        // Enrichment is bounded to one page of hotels (not the whole table).
        Mono<List<Hotel>> rows = hotelPort.search(criteria)
                .flatMapSequential(this::enrich)
                .collectList();

        return Mono.zip(rows, total)
                .map(t -> {
                    long totalRecords = t.getT2();
                    int totalPages = (int) Math.ceil((double) totalRecords / safeSize);
                    return HotelListResponseDto.builder()
                            .message("Hotels retrieved successfully")
                            .page(safePage)
                            .size(safeSize)
                            .totalRecords(totalRecords)
                            .totalPages(totalPages)
                            .hotelData(t.getT1())
                            .build();
                })
                .doOnError(e -> log.error("Error finding hotels (search='{}', facilities={}): {}", search, facs, e.getMessage()));
    }

    // ----------------------------------- Owner / Admin lists ------------------------------

    @Override
    public Mono<HotelListResponseDto> findOwnerHotels(String userId) {
        return hotelPort.findByOwner(userId)
                .flatMapSequential(this::enrich)
                .collectList()
                .map(list -> HotelListResponseDto.builder()
                        .message("Hotels retrieved successfully")
                        .page(0).size(list.size()).totalRecords(list.size()).totalPages(1)
                        .hotelData(list)
                        .build())
                .doOnError(e -> log.error("Error finding hotels for owner {}: {}", userId, e.getMessage()));
    }

    @Override
    public Mono<com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto.AdminHotelListResponseDto> findAdminHotels() {
        return hotelPort.findAllForAdmin()
                .collectList()
                .map(list -> com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto.AdminHotelListResponseDto.builder()
                        .message("Hotels retrieved successfully")
                        .totalRecords(list.size())
                        .hotelData(list)
                        .build())
                .doOnError(e -> log.error("Error building admin hotel list: {}", e.getMessage()));
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
        Mono<List<Image>> hotelImages =
                imagePort.findAllByHotelIdAndRoomIdIsNull(hotel.getId()).collectList();

        Mono<List<AssignedFacility>> hotelFacilities =
                facilityPort.findHotelFacilities(hotel.getId()).collectList();

        Mono<List<Room>> rooms =
                roomPort.findAllByHotelId(hotel.getId())
                        .collectList()
                        .flatMap(this::attachRoomDetails);

        return Mono.zip(hotelImages, rooms, hotelFacilities)
                .map(tuple -> {
                    hotel.setImages(tuple.getT1());
                    hotel.setRooms(tuple.getT2());
                    hotel.setFacilities(tuple.getT3());
                    hotel.setCoverImageUrl(resolveCoverImageUrl(tuple.getT1()));
                    return hotel;
                });
    }

    /**
     * Batch-attaches images + facilities to a hotel's rooms with no per-room N+1: one batched image
     * query and one batched facility query (2 queries) for the whole room set.
     */
    private Mono<List<Room>> attachRoomDetails(List<Room> rooms) {
        if (rooms.isEmpty()) {
            return Mono.just(rooms);
        }
        List<String> roomIds = rooms.stream().map(Room::getId).toList();
        return Mono.zip(
                        imagePort.findAllByRoomIdIn(roomIds).collectMultimap(Image::getRoomId),
                        facilityPort.findRoomFacilitiesByRoomIds(roomIds))
                .map(t -> {
                    var imagesByRoom = t.getT1();
                    var facilitiesByRoom = t.getT2();
                    rooms.forEach(room -> {
                        room.setImages(new ArrayList<>(imagesByRoom.getOrDefault(room.getId(), List.of())));
                        room.setFacilities(facilitiesByRoom.getOrDefault(room.getId(), List.of()));
                    });
                    return rooms;
                });
    }

    /**
     * Picks the single image to show in a hotel list/grid: the first image flagged primary, else
     * the first hotel-level image by display order (the list arrives already ordered). Prefers the
     * lightweight thumbnail, falling back to the full image URL. Returns null if the hotel has no
     * hotel-level images.
     */
    private String resolveCoverImageUrl(java.util.List<com.kaptaitourist.kaptaitourist.image.domain.Image> hotelImages) {
        if (hotelImages == null || hotelImages.isEmpty()) {
            return null;
        }
        var chosen = hotelImages.stream()
                .filter(com.kaptaitourist.kaptaitourist.image.domain.Image::isPrimary)
                .findFirst()
                .orElse(hotelImages.get(0));
        return (chosen.getThumbnailUrl() != null && !chosen.getThumbnailUrl().isBlank())
                ? chosen.getThumbnailUrl()
                : chosen.getFileUrl();
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
