package com.kaptaitourist.kaptaitourist.Room.application.service;

import com.kaptaitourist.kaptaitourist.Room.adapter.in.web.dto.RoomListResponseDto;
import com.kaptaitourist.kaptaitourist.Room.adapter.in.web.dto.RoomRequestDto;
import com.kaptaitourist.kaptaitourist.Room.adapter.in.web.dto.RoomResponseDto;
import com.kaptaitourist.kaptaitourist.Room.application.port.in.RoomUseCase;
import com.kaptaitourist.kaptaitourist.Room.application.port.out.RoomPort;
import com.kaptaitourist.kaptaitourist.Room.domain.Room;
import com.kaptaitourist.kaptaitourist.core.exception.HotelNotFoundException;
import com.kaptaitourist.kaptaitourist.core.exception.RoomNotFoundException;
import com.kaptaitourist.kaptaitourist.core.exception.ValidationException;
import com.kaptaitourist.kaptaitourist.facility.application.port.out.FacilityPort;
import com.kaptaitourist.kaptaitourist.hotel.application.port.out.HotelPort;
import com.kaptaitourist.kaptaitourist.image.application.port.in.ImageUseCase;
import com.kaptaitourist.kaptaitourist.image.application.port.out.ImagePort;
import com.kaptaitourist.kaptaitourist.image.domain.Image;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomService implements RoomUseCase {

    private final RoomPort roomPort;
    private final ImagePort imagePort;
    private final ImageUseCase imageUseCase;
    private final HotelPort hotelPort;
    private final FacilityPort facilityPort;

    // ----------------------------------- Create -------------------------------------------

    @Override
    public Mono<RoomResponseDto> createRoom(String hotelId, RoomRequestDto dto) {
        if (dto.getRoomName() == null || dto.getRoomName().isBlank())
            return Mono.error(new ValidationException("Room name is required"));

        return hotelPort.existsById(hotelId)
                .flatMap(hotelExists -> {
                    if (!hotelExists)
                        return Mono.error(new HotelNotFoundException("Hotel not found with id: " + hotelId));

                    Room room = Room.builder()
                            .hotelId(hotelId)
                            .roomName(dto.getRoomName())
                            .capacity(dto.getCapacity() != null ? dto.getCapacity() : 1)
                            .totalUnits(dto.getTotalUnits() != null ? dto.getTotalUnits() : 1)
                            .roomType(dto.getRoomType())
                            .isAirConditioned(Boolean.TRUE.equals(dto.getIsAirConditioned()))
                            .description(dto.getDescription())
                            .prerequisites(dto.getPrerequisites())
                            .pricePerNight(dto.getPricePerNight() != null ? dto.getPricePerNight() : BigDecimal.ZERO)
                            .discount(dto.getDiscount() != null ? dto.getDiscount() : BigDecimal.ZERO)
                            .isAvailable(dto.getIsAvailable() == null || dto.getIsAvailable())
                            .createdBy(dto.getCreatedBy())
                            .createdAt(LocalDateTime.now())
                            .build();
                    return roomPort.save(room);
                })
                .map(saved -> {
                    saved.setImages(List.of());     // a freshly created room has no images yet
                    saved.setFacilities(List.of()); // nor any facilities assigned yet
                    return RoomResponseDto.builder()
                            .message("Room created successfully")
                            .roomData(saved)
                            .build();
                })
                .doOnSuccess(r -> log.info("Created room id: {} for hotelId: {}", r.getRoomData().getId(), hotelId))
                .doOnError(e -> log.error("Error creating room for hotelId {}: {}", hotelId, e.getMessage()));
    }

    // ----------------------------------- Find all by hotel --------------------------------

    @Override
    public Mono<RoomListResponseDto> findAllByHotelId(String hotelId) {
        return roomPort.findAllByHotelId(hotelId)
                .collectList()
                .flatMap(this::attachDetails)
                .map(rooms -> RoomListResponseDto.builder()
                        .message("Rooms retrieved successfully")
                        .totalRecords(rooms.size())
                        .roomData(rooms)
                        .build())
                .doOnError(e -> log.error("Error finding rooms for hotelId {}: {}", hotelId, e.getMessage()));
    }

    // ----------------------------------- Find by id ---------------------------------------

    @Override
    public Mono<RoomResponseDto> findById(String hotelId, String roomId) {
        return roomPort.findByIdAndHotelId(roomId, hotelId)
                .switchIfEmpty(Mono.error(new RoomNotFoundException(
                        "Room not found with id: " + roomId + " for hotel: " + hotelId)))
                .flatMap(this::attachDetails)
                .map(room -> RoomResponseDto.builder()
                        .message("Room retrieved successfully")
                        .roomData(room)
                        .build())
                .doOnError(e -> log.error("Error finding room id {} for hotelId {}: {}", roomId, hotelId, e.getMessage()));
    }

    // ----------------------------------- Update -------------------------------------------

    @Override
    public Mono<RoomResponseDto> updateRoom(String hotelId, String roomId, RoomRequestDto dto) {
        if (dto.getRoomName() == null || dto.getRoomName().isBlank())
            return Mono.error(new ValidationException("Room name is required"));

        return roomPort.findByIdAndHotelId(roomId, hotelId)
                .switchIfEmpty(Mono.error(new RoomNotFoundException(
                        "Room not found with id: " + roomId + " for hotel: " + hotelId)))
                .flatMap(existing -> {
                    // Full replace (PUT), preserving identity, hotel, version and creation audit.
                    Room updated = Room.builder()
                            .id(existing.getId())
                            .hotelId(existing.getHotelId())
                            .roomName(dto.getRoomName())
                            .capacity(dto.getCapacity() != null ? dto.getCapacity() : 1)
                            .totalUnits(dto.getTotalUnits() != null ? dto.getTotalUnits() : 1)
                            .roomType(dto.getRoomType())
                            .isAirConditioned(Boolean.TRUE.equals(dto.getIsAirConditioned()))
                            .description(dto.getDescription())
                            .prerequisites(dto.getPrerequisites())
                            .pricePerNight(dto.getPricePerNight() != null ? dto.getPricePerNight() : BigDecimal.ZERO)
                            .discount(dto.getDiscount() != null ? dto.getDiscount() : BigDecimal.ZERO)
                            .isAvailable(dto.getIsAvailable() == null || dto.getIsAvailable())
                            .version(existing.getVersion())
                            .createdBy(existing.getCreatedBy())
                            .createdAt(existing.getCreatedAt())
                            .updatedBy(dto.getUpdatedBy())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return roomPort.save(updated);
                })
                .flatMap(this::attachDetails)
                .map(room -> RoomResponseDto.builder()
                        .message("Room updated successfully")
                        .roomData(room)
                        .build())
                .doOnError(e -> log.error("Error updating room id {} for hotelId {}: {}", roomId, hotelId, e.getMessage()));
    }

    // ----------------------------------- Delete -------------------------------------------

    @Override
    public Mono<Void> deleteRoom(String hotelId, String roomId) {
        return roomPort.findByIdAndHotelId(roomId, hotelId)
                .switchIfEmpty(Mono.error(new RoomNotFoundException(
                        "Room not found with id: " + roomId + " for hotel: " + hotelId)))
                // Remove the room's images (storage objects + rows) BEFORE the room delete,
                // otherwise the FK cascade would drop the rows and orphan the storage files.
                .flatMap(room -> imageUseCase.deleteAllByRoomId(roomId)
                        .then(roomPort.deleteById(roomId)))
                .doOnSuccess(v -> log.info("Deleted room id: {} for hotelId: {}", roomId, hotelId))
                .doOnError(e -> log.error("Error deleting room id {} for hotelId {}: {}", roomId, hotelId, e.getMessage()));
    }

    // ----------------------------------- Helpers ------------------------------------------

    /** Attach the gallery and assigned facilities to a single room. */
    private Mono<Room> attachDetails(Room room) {
        return Mono.zip(
                        imagePort.findAllByRoomId(room.getId()).collectList(),
                        facilityPort.findRoomFacilities(room.getId()).collectList())
                .map(t -> {
                    room.setImages(t.getT1());
                    room.setFacilities(t.getT2());
                    return room;
                });
    }

    /**
     * Batch-attach galleries + facilities to many rooms. Images use a single batched query to
     * avoid N+1; facilities are fetched per room (no batch port method), bounded to one hotel's
     * rooms per call.
     */
    private Mono<List<Room>> attachDetails(List<Room> rooms) {
        if (rooms.isEmpty())
            return Mono.just(rooms);

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
}
