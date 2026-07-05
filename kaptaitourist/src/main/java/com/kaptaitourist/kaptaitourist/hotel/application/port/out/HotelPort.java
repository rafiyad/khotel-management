package com.kaptaitourist.kaptaitourist.hotel.application.port.out;

import com.kaptaitourist.kaptaitourist.hotel.domain.AdminHotelView;
import com.kaptaitourist.kaptaitourist.hotel.domain.Hotel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface HotelPort {
    Mono<Hotel> save(Hotel hotel);

    /** Hotels owned by the given user (via khotel_hotel_owner), ordered by name. */
    Flux<Hotel> findByOwner(String userId);

    /** All hotels with owners + room-type/booking counts (admin oversight; no image enrichment). */
    Flux<AdminHotelView> findAllForAdmin();

    /** One page of hotels matching the criteria (name / facility ALL-match / date availability). */
    Flux<Hotel> search(HotelSearchCriteria criteria);

    /** Total hotels matching the same criteria (ignores page size/offset). */
    Mono<Long> count(HotelSearchCriteria criteria);

    Mono<Hotel> findById(String id);
    Mono<Void> deleteById(String id);
    Mono<Boolean> existsById(String id);
}
