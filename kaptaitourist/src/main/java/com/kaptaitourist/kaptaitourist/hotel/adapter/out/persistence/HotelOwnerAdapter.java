package com.kaptaitourist.kaptaitourist.hotel.adapter.out.persistence;

import com.kaptaitourist.kaptaitourist.core.security.OwnershipChecker;
import com.kaptaitourist.kaptaitourist.hotel.adapter.out.persistence.entity.HotelOwnerEntity;
import com.kaptaitourist.kaptaitourist.hotel.adapter.out.persistence.repository.HotelOwnerRepository;
import com.kaptaitourist.kaptaitourist.hotel.application.port.out.HotelOwnerPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class HotelOwnerAdapter implements HotelOwnerPort, OwnershipChecker {

    private final HotelOwnerRepository hotelOwnerRepository;

    @Override
    public Mono<Void> assignOwner(String userId, String hotelId) {
        return hotelOwnerRepository.existsByUserIdAndHotelId(userId, hotelId)
                .flatMap(exists -> exists
                        ? Mono.empty()
                        : hotelOwnerRepository.save(HotelOwnerEntity.builder()
                                .userId(userId)
                                .hotelId(hotelId)
                                .createdAt(LocalDateTime.now())
                                .build())
                        .then())
                .doOnError(e -> log.error("Error assigning owner {} to hotel {}: {}", userId, hotelId, e.getMessage()));
    }

    @Override
    public Mono<Boolean> ownsHotel(String userId, String hotelId) {
        if (userId == null || hotelId == null) {
            return Mono.just(false);
        }
        return hotelOwnerRepository.existsByUserIdAndHotelId(userId, hotelId);
    }
}
