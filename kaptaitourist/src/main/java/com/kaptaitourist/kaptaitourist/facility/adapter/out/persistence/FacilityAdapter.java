package com.kaptaitourist.kaptaitourist.facility.adapter.out.persistence;

import com.kaptaitourist.kaptaitourist.facility.adapter.out.persistence.entity.FacilityEntity;
import com.kaptaitourist.kaptaitourist.facility.adapter.out.persistence.entity.HotelFacilityEntity;
import com.kaptaitourist.kaptaitourist.facility.adapter.out.persistence.entity.RoomFacilityEntity;
import com.kaptaitourist.kaptaitourist.facility.adapter.out.persistence.repository.FacilityRepository;
import com.kaptaitourist.kaptaitourist.facility.adapter.out.persistence.repository.HotelFacilityRepository;
import com.kaptaitourist.kaptaitourist.facility.adapter.out.persistence.repository.RoomFacilityRepository;
import com.kaptaitourist.kaptaitourist.facility.application.port.out.FacilityPort;
import com.kaptaitourist.kaptaitourist.facility.domain.AssignedFacility;
import com.kaptaitourist.kaptaitourist.facility.domain.Facility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class FacilityAdapter implements FacilityPort {

    private final FacilityRepository facilityRepository;
    private final HotelFacilityRepository hotelFacilityRepository;
    private final RoomFacilityRepository roomFacilityRepository;
    private final ModelMapper modelMapper;

    // ---- Catalog ----

    @Override
    public Mono<Facility> save(Facility facility) {
        return facilityRepository.save(modelMapper.map(facility, FacilityEntity.class))
                .map(entity -> modelMapper.map(entity, Facility.class))
                .doOnError(e -> log.error("Error saving facility: {}", e.getMessage()));
    }

    @Override
    public Flux<Facility> findAll() {
        return facilityRepository.findAllOrdered()
                .map(entity -> modelMapper.map(entity, Facility.class));
    }

    @Override
    public Mono<Facility> findById(String id) {
        return facilityRepository.findById(id)
                .map(entity -> modelMapper.map(entity, Facility.class));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return facilityRepository.deleteById(id);
    }

    @Override
    public Mono<Boolean> existsById(String id) {
        return facilityRepository.existsById(id);
    }

    // ---- Hotel links ----

    @Override
    public Mono<AssignedFacility> assignToHotel(String hotelId, String facilityId, Boolean isComplimentary,
                                                Boolean isAvailable, BigDecimal additionalCharge, String notes, String createdBy) {
        return hotelFacilityRepository.findByHotelIdAndFacilityId(hotelId, facilityId)
                .flatMap(existing -> {
                    existing.setIsComplimentary(isComplimentary == null || isComplimentary);
                    existing.setIsAvailable(isAvailable == null || isAvailable);
                    existing.setAdditionalCharge(additionalCharge);
                    existing.setNotes(notes);
                    return hotelFacilityRepository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> hotelFacilityRepository.save(HotelFacilityEntity.builder()
                        .hotelId(hotelId)
                        .facilityId(facilityId)
                        .isComplimentary(isComplimentary == null || isComplimentary)
                        .isAvailable(isAvailable == null || isAvailable)
                        .additionalCharge(additionalCharge)
                        .notes(notes)
                        .createdBy(createdBy)
                        .createdAt(LocalDateTime.now())
                        .build())))
                .flatMap(link -> facilityRepository.findById(facilityId)
                        .map(fac -> toAssigned(fac, link.getIsComplimentary(), link.getIsAvailable(), link.getAdditionalCharge(), link.getNotes())));
    }

    @Override
    public Flux<AssignedFacility> findHotelFacilities(String hotelId) {
        return hotelFacilityRepository.findAllByHotelId(hotelId)
                .flatMap(link -> facilityRepository.findById(link.getFacilityId())
                        .map(fac -> toAssigned(fac, link.getIsComplimentary(), link.getIsAvailable(), link.getAdditionalCharge(), link.getNotes())));
    }

    @Override
    public Mono<Boolean> unassignFromHotel(String hotelId, String facilityId) {
        return hotelFacilityRepository.deleteByHotelIdAndFacilityId(hotelId, facilityId)
                .map(count -> count != null && count > 0);
    }

    // ---- Room links ----

    @Override
    public Mono<AssignedFacility> assignToRoom(String roomId, String facilityId, Boolean isComplimentary,
                                               Boolean isAvailable, BigDecimal additionalCharge, String notes, String createdBy) {
        return roomFacilityRepository.findByRoomIdAndFacilityId(roomId, facilityId)
                .flatMap(existing -> {
                    existing.setIsComplimentary(isComplimentary == null || isComplimentary);
                    existing.setIsAvailable(isAvailable == null || isAvailable);
                    existing.setAdditionalCharge(additionalCharge);
                    existing.setNotes(notes);
                    return roomFacilityRepository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> roomFacilityRepository.save(RoomFacilityEntity.builder()
                        .roomId(roomId)
                        .facilityId(facilityId)
                        .isComplimentary(isComplimentary == null || isComplimentary)
                        .isAvailable(isAvailable == null || isAvailable)
                        .additionalCharge(additionalCharge)
                        .notes(notes)
                        .createdBy(createdBy)
                        .createdAt(LocalDateTime.now())
                        .build())))
                .flatMap(link -> facilityRepository.findById(facilityId)
                        .map(fac -> toAssigned(fac, link.getIsComplimentary(), link.getIsAvailable(), link.getAdditionalCharge(), link.getNotes())));
    }

    @Override
    public Flux<AssignedFacility> findRoomFacilities(String roomId) {
        return roomFacilityRepository.findAllByRoomId(roomId)
                .flatMap(link -> facilityRepository.findById(link.getFacilityId())
                        .map(fac -> toAssigned(fac, link.getIsComplimentary(), link.getIsAvailable(), link.getAdditionalCharge(), link.getNotes())));
    }

    @Override
    public Mono<Map<String, List<AssignedFacility>>> findRoomFacilitiesByRoomIds(List<String> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return Mono.just(Map.of());
        }
        // Two queries total (links IN roomIds, then facilities IN facilityIds), instead of the
        // per-room / per-link N+1 of findRoomFacilities. Grouped back to roomId in memory.
        return roomFacilityRepository.findAllByRoomIdIn(roomIds)
                .collectList()
                .flatMap(links -> {
                    if (links.isEmpty()) {
                        return Mono.just(Map.of());
                    }
                    List<String> facilityIds = links.stream()
                            .map(RoomFacilityEntity::getFacilityId)
                            .distinct()
                            .toList();
                    return facilityRepository.findAllById(facilityIds)
                            .collectMap(FacilityEntity::getId)
                            .map(facById -> links.stream()
                                    .filter(link -> facById.containsKey(link.getFacilityId()))
                                    .collect(Collectors.groupingBy(
                                            RoomFacilityEntity::getRoomId,
                                            Collectors.mapping(link -> toAssigned(
                                                            facById.get(link.getFacilityId()),
                                                            link.getIsComplimentary(),
                                                            link.getIsAvailable(),
                                                            link.getAdditionalCharge(),
                                                            link.getNotes()),
                                                    Collectors.toList()))));
                });
    }

    @Override
    public Mono<Boolean> unassignFromRoom(String roomId, String facilityId) {
        return roomFacilityRepository.deleteByRoomIdAndFacilityId(roomId, facilityId)
                .map(count -> count != null && count > 0);
    }

    // ---- Helper ----

    private AssignedFacility toAssigned(FacilityEntity fac, Boolean isComplimentary, Boolean isAvailable,
                                        BigDecimal additionalCharge, String notes) {
        return AssignedFacility.builder()
                .facility(modelMapper.map(fac, Facility.class))
                .isComplimentary(isComplimentary)
                .isAvailable(isAvailable)
                .additionalCharge(additionalCharge)
                .notes(notes)
                .build();
    }
}
