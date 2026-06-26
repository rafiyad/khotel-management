package com.kaptaitourist.kaptaitourist.facility.application.service;

import com.kaptaitourist.kaptaitourist.Room.application.port.out.RoomPort;
import com.kaptaitourist.kaptaitourist.core.exception.FacilityNotFoundException;
import com.kaptaitourist.kaptaitourist.core.exception.HotelNotFoundException;
import com.kaptaitourist.kaptaitourist.core.exception.RoomNotFoundException;
import com.kaptaitourist.kaptaitourist.core.exception.ValidationException;
import com.kaptaitourist.kaptaitourist.facility.adapter.in.web.dto.AssignedFacilityListResponseDto;
import com.kaptaitourist.kaptaitourist.facility.adapter.in.web.dto.AssignedFacilityResponseDto;
import com.kaptaitourist.kaptaitourist.facility.adapter.in.web.dto.FacilityAssignmentRequestDto;
import com.kaptaitourist.kaptaitourist.facility.adapter.in.web.dto.FacilityListResponseDto;
import com.kaptaitourist.kaptaitourist.facility.adapter.in.web.dto.FacilityRequestDto;
import com.kaptaitourist.kaptaitourist.facility.adapter.in.web.dto.FacilityResponseDto;
import com.kaptaitourist.kaptaitourist.facility.application.port.in.FacilityUseCase;
import com.kaptaitourist.kaptaitourist.facility.application.port.out.FacilityPort;
import com.kaptaitourist.kaptaitourist.facility.domain.Facility;
import com.kaptaitourist.kaptaitourist.hotel.application.port.out.HotelPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class FacilityService implements FacilityUseCase {

    private final FacilityPort facilityPort;
    private final HotelPort hotelPort;
    private final RoomPort roomPort;

    // ===================================== Catalog ========================================

    @Override
    public Mono<FacilityResponseDto> createFacility(FacilityRequestDto dto) {
        if (dto.getName() == null || dto.getName().isBlank())
            return Mono.error(new ValidationException("Facility name is required"));

        Facility facility = Facility.builder()
                .name(dto.getName())
                .category(dto.getCategory())
                .icon(dto.getIcon())
                .description(dto.getDescription())
                .appliesTo(normalizeAppliesTo(dto.getAppliesTo()))
                .isActive(dto.getIsActive() == null || dto.getIsActive())
                .createdBy(dto.getCreatedBy())
                .createdAt(LocalDateTime.now())
                .build();

        return facilityPort.save(facility)
                .map(saved -> FacilityResponseDto.builder()
                        .message("Facility created successfully")
                        .facilityData(saved)
                        .build())
                .doOnError(e -> log.error("Error creating facility: {}", e.getMessage()));
    }

    @Override
    public Mono<FacilityListResponseDto> findAll() {
        return facilityPort.findAll()
                .collectList()
                .map(list -> FacilityListResponseDto.builder()
                        .message("Facilities retrieved successfully")
                        .totalRecords(list.size())
                        .facilityData(list)
                        .build());
    }

    @Override
    public Mono<FacilityResponseDto> findById(String id) {
        return facilityPort.findById(id)
                .switchIfEmpty(Mono.error(new FacilityNotFoundException("Facility not found with id: " + id)))
                .map(facility -> FacilityResponseDto.builder()
                        .message("Facility retrieved successfully")
                        .facilityData(facility)
                        .build());
    }

    @Override
    public Mono<FacilityResponseDto> updateFacility(String id, FacilityRequestDto dto) {
        if (dto.getName() == null || dto.getName().isBlank())
            return Mono.error(new ValidationException("Facility name is required"));

        return facilityPort.findById(id)
                .switchIfEmpty(Mono.error(new FacilityNotFoundException("Facility not found with id: " + id)))
                .flatMap(existing -> {
                    Facility updated = Facility.builder()
                            .id(existing.getId())
                            .name(dto.getName())
                            .category(dto.getCategory())
                            .icon(dto.getIcon())
                            .description(dto.getDescription())
                            .appliesTo(normalizeAppliesTo(dto.getAppliesTo()))
                            .isActive(dto.getIsActive() == null || dto.getIsActive())
                            .version(existing.getVersion())
                            .createdBy(existing.getCreatedBy())
                            .createdAt(existing.getCreatedAt())
                            .updatedBy(dto.getUpdatedBy())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return facilityPort.save(updated);
                })
                .map(saved -> FacilityResponseDto.builder()
                        .message("Facility updated successfully")
                        .facilityData(saved)
                        .build());
    }

    @Override
    public Mono<Void> deleteFacility(String id) {
        return facilityPort.existsById(id)
                .flatMap(exists -> exists
                        ? facilityPort.deleteById(id)
                        : Mono.error(new FacilityNotFoundException("Facility not found with id: " + id)))
                .doOnSuccess(v -> log.info("Deleted facility id: {}", id));
    }

    // ===================================== Hotel assignment ===============================

    @Override
    public Mono<AssignedFacilityResponseDto> assignToHotel(String hotelId, FacilityAssignmentRequestDto dto) {
        if (dto.getFacilityId() == null || dto.getFacilityId().isBlank())
            return Mono.error(new ValidationException("facilityId is required"));

        return hotelPort.existsById(hotelId)
                .flatMap(exists -> exists
                        ? facilityPort.findById(dto.getFacilityId())
                                .switchIfEmpty(Mono.error(new FacilityNotFoundException(
                                        "Facility not found with id: " + dto.getFacilityId())))
                                .flatMap(facility -> validateScope(facility, "HOTEL")
                                        .then(facilityPort.assignToHotel(hotelId, dto.getFacilityId(),
                                                dto.getIsComplimentary(), dto.getAdditionalCharge(),
                                                dto.getNotes(), dto.getCreatedBy())))
                        : Mono.error(new HotelNotFoundException("Hotel not found with id: " + hotelId)))
                .map(assigned -> AssignedFacilityResponseDto.builder()
                        .message("Facility assigned to hotel")
                        .data(assigned)
                        .build())
                .doOnError(e -> log.error("Error assigning facility to hotel {}: {}", hotelId, e.getMessage()));
    }

    @Override
    public Mono<AssignedFacilityListResponseDto> findHotelFacilities(String hotelId) {
        return facilityPort.findHotelFacilities(hotelId)
                .collectList()
                .map(list -> AssignedFacilityListResponseDto.builder()
                        .message("Hotel facilities retrieved successfully")
                        .totalRecords(list.size())
                        .data(list)
                        .build());
    }

    @Override
    public Mono<Void> unassignFromHotel(String hotelId, String facilityId) {
        return facilityPort.unassignFromHotel(hotelId, facilityId)
                .flatMap(removed -> removed
                        ? Mono.empty()
                        : Mono.error(new FacilityNotFoundException(
                                "Facility " + facilityId + " is not assigned to hotel " + hotelId)))
                .then();
    }

    // ===================================== Room assignment ================================

    @Override
    public Mono<AssignedFacilityResponseDto> assignToRoom(String hotelId, String roomId, FacilityAssignmentRequestDto dto) {
        if (dto.getFacilityId() == null || dto.getFacilityId().isBlank())
            return Mono.error(new ValidationException("facilityId is required"));

        return roomPort.findByIdAndHotelId(roomId, hotelId)
                .switchIfEmpty(Mono.error(new RoomNotFoundException(
                        "Room not found with id: " + roomId + " for hotel: " + hotelId)))
                .flatMap(room -> facilityPort.findById(dto.getFacilityId())
                        .switchIfEmpty(Mono.error(new FacilityNotFoundException(
                                "Facility not found with id: " + dto.getFacilityId())))
                        .flatMap(facility -> validateScope(facility, "ROOM")
                                .then(facilityPort.assignToRoom(roomId, dto.getFacilityId(),
                                        dto.getIsComplimentary(), dto.getAdditionalCharge(),
                                        dto.getNotes(), dto.getCreatedBy()))))
                .map(assigned -> AssignedFacilityResponseDto.builder()
                        .message("Facility assigned to room")
                        .data(assigned)
                        .build())
                .doOnError(e -> log.error("Error assigning facility to room {}: {}", roomId, e.getMessage()));
    }

    @Override
    public Mono<AssignedFacilityListResponseDto> findRoomFacilities(String roomId) {
        return facilityPort.findRoomFacilities(roomId)
                .collectList()
                .map(list -> AssignedFacilityListResponseDto.builder()
                        .message("Room facilities retrieved successfully")
                        .totalRecords(list.size())
                        .data(list)
                        .build());
    }

    @Override
    public Mono<Void> unassignFromRoom(String roomId, String facilityId) {
        return facilityPort.unassignFromRoom(roomId, facilityId)
                .flatMap(removed -> removed
                        ? Mono.empty()
                        : Mono.error(new FacilityNotFoundException(
                                "Facility " + facilityId + " is not assigned to room " + roomId)))
                .then();
    }

    // ===================================== Helpers ========================================

    private String normalizeAppliesTo(String appliesTo) {
        if (appliesTo == null || appliesTo.isBlank()) return "BOTH";
        String v = appliesTo.trim().toUpperCase();
        if (!v.equals("HOTEL") && !v.equals("ROOM") && !v.equals("BOTH"))
            throw new ValidationException("appliesTo must be one of HOTEL, ROOM, BOTH");
        return v;
    }

    /** Fails if the facility's appliesTo does not permit assignment at the given scope (HOTEL/ROOM). */
    private Mono<Void> validateScope(Facility facility, String scope) {
        String a = facility.getAppliesTo();
        boolean ok = a == null || "BOTH".equalsIgnoreCase(a) || scope.equalsIgnoreCase(a);
        if (!ok)
            return Mono.error(new ValidationException(
                    "Facility '" + facility.getName() + "' cannot be assigned to a " + scope.toLowerCase()
                            + " (appliesTo=" + a + ")"));
        return Mono.empty();
    }
}
