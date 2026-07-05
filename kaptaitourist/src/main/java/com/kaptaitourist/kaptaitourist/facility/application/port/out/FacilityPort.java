package com.kaptaitourist.kaptaitourist.facility.application.port.out;

import com.kaptaitourist.kaptaitourist.facility.domain.AssignedFacility;
import com.kaptaitourist.kaptaitourist.facility.domain.Facility;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface FacilityPort {
    // ---- Catalog ----
    Mono<Facility> save(Facility facility);
    Flux<Facility> findAll();
    Mono<Facility> findById(String id);
    Mono<Void> deleteById(String id);
    Mono<Boolean> existsById(String id);

    // ---- Hotel links ----
    Mono<AssignedFacility> assignToHotel(String hotelId, String facilityId, Boolean isComplimentary,
                                         Boolean isAvailable, BigDecimal additionalCharge, String notes, String createdBy);
    Flux<AssignedFacility> findHotelFacilities(String hotelId);
    Mono<Boolean> unassignFromHotel(String hotelId, String facilityId);

    // ---- Room links ----
    Mono<AssignedFacility> assignToRoom(String roomId, String facilityId, Boolean isComplimentary,
                                        Boolean isAvailable, BigDecimal additionalCharge, String notes, String createdBy);
    Flux<AssignedFacility> findRoomFacilities(String roomId);
    /** Batched: assigned facilities for many rooms at once, keyed by roomId (rooms with none are absent). */
    Mono<Map<String, List<AssignedFacility>>> findRoomFacilitiesByRoomIds(List<String> roomIds);
    Mono<Boolean> unassignFromRoom(String roomId, String facilityId);
}
