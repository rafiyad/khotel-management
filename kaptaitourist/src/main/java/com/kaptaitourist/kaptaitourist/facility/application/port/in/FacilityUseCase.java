package com.kaptaitourist.kaptaitourist.facility.application.port.in;

import com.kaptaitourist.kaptaitourist.facility.adapter.in.web.dto.AssignedFacilityListResponseDto;
import com.kaptaitourist.kaptaitourist.facility.adapter.in.web.dto.AssignedFacilityResponseDto;
import com.kaptaitourist.kaptaitourist.facility.adapter.in.web.dto.FacilityAssignmentRequestDto;
import com.kaptaitourist.kaptaitourist.facility.adapter.in.web.dto.FacilityListResponseDto;
import com.kaptaitourist.kaptaitourist.facility.adapter.in.web.dto.FacilityRequestDto;
import com.kaptaitourist.kaptaitourist.facility.adapter.in.web.dto.FacilityResponseDto;
import reactor.core.publisher.Mono;

public interface FacilityUseCase {

    // ---- Catalog ---- (creatorUserId from token; isAdmin bypasses the owner-only-own check)
    Mono<FacilityResponseDto> createFacility(FacilityRequestDto dto, String creatorUserId);
    Mono<FacilityListResponseDto> findAll();
    Mono<FacilityResponseDto> findById(String id);
    Mono<FacilityResponseDto> updateFacility(String id, FacilityRequestDto dto, String userId, boolean isAdmin);
    Mono<Void> deleteFacility(String id, String userId, boolean isAdmin);

    // ---- Hotel assignment ----
    Mono<AssignedFacilityResponseDto> assignToHotel(String hotelId, FacilityAssignmentRequestDto dto);
    Mono<AssignedFacilityListResponseDto> findHotelFacilities(String hotelId);
    Mono<Void> unassignFromHotel(String hotelId, String facilityId);

    // ---- Room assignment ----
    Mono<AssignedFacilityResponseDto> assignToRoom(String hotelId, String roomId, FacilityAssignmentRequestDto dto);
    Mono<AssignedFacilityListResponseDto> findRoomFacilities(String roomId);
    Mono<Void> unassignFromRoom(String roomId, String facilityId);
}
