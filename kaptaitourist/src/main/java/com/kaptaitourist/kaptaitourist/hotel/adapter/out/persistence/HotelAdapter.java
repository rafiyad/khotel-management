package com.kaptaitourist.kaptaitourist.hotel.adapter.out.persistence;

import com.kaptaitourist.kaptaitourist.hotel.adapter.out.persistence.entity.HotelEntity;
import com.kaptaitourist.kaptaitourist.hotel.adapter.out.persistence.repository.HotelRepository;
import com.kaptaitourist.kaptaitourist.hotel.application.port.out.HotelPort;
import com.kaptaitourist.kaptaitourist.hotel.application.port.out.HotelSearchCriteria;
import com.kaptaitourist.kaptaitourist.hotel.domain.AdminHotelView;
import com.kaptaitourist.kaptaitourist.hotel.domain.Hotel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class HotelAdapter implements HotelPort {

    private final HotelRepository hotelRepository;
    private final DatabaseClient db;
    private final ModelMapper modelMapper;

    @Override
    public Mono<Hotel> save(Hotel hotel) {
        log.info("Saving hotel to db: {}", hotel.getName());
        return hotelRepository.save(modelMapper.map(hotel, HotelEntity.class))
                .map(entity -> modelMapper.map(entity, Hotel.class))
                .doOnSuccess(saved -> log.info("Saved hotel with id: {}", saved.getId()))
                .doOnError(e -> log.error("Error saving hotel: {}", e.getMessage()));
    }

    @Override
    public Flux<Hotel> search(HotelSearchCriteria c) {
        Filter f = buildFilter(c);
        String sql = "SELECT id FROM khotel_hotel WHERE " + f.where + " ORDER BY name LIMIT :size OFFSET :offset";
        DatabaseClient.GenericExecuteSpec spec = bindAll(db.sql(sql), f.binds)
                .bind("size", c.size()).bind("offset", c.offset());

        // Page the IDs (ordered), then load full rows and re-order to match — keeps the ORDER BY.
        return spec.map((row, md) -> row.get("id", String.class)).all()
                .collectList()
                .flatMapMany(ids -> ids.isEmpty()
                        ? Flux.empty()
                        : hotelRepository.findAllById(ids)
                                .collectMap(HotelEntity::getId)
                                .flatMapMany(byId -> Flux.fromIterable(ids)
                                        .map(byId::get)
                                        .filter(Objects::nonNull)
                                        .map(entity -> modelMapper.map(entity, Hotel.class))))
                .doOnError(e -> log.error("Error searching hotels: {}", e.getMessage()));
    }

    @Override
    public Mono<Long> count(HotelSearchCriteria c) {
        Filter f = buildFilter(c);
        String sql = "SELECT COUNT(*) FROM khotel_hotel WHERE " + f.where;
        return bindAll(db.sql(sql), f.binds)
                .map((row, md) -> row.get(0, Long.class))
                .one()
                .doOnError(e -> log.error("Error counting hotels: {}", e.getMessage()));
    }

    /** Builds the WHERE fragment + the exact named binds it references (nothing extra). */
    private Filter buildFilter(HotelSearchCriteria c) {
        Map<String, Object> binds = new LinkedHashMap<>();
        StringBuilder where = new StringBuilder("name ILIKE '%' || :search || '%'");
        binds.put("search", c.search() == null ? "" : c.search().trim());

        List<String> facilities = c.facilityIds() == null ? List.of() : c.facilityIds();
        if (!facilities.isEmpty()) {
            List<String> placeholders = new ArrayList<>();
            for (int i = 0; i < facilities.size(); i++) {
                String p = "fac" + i;
                placeholders.add(":" + p);
                binds.put(p, facilities.get(i));
            }
            where.append(" AND id IN (SELECT hf.hotel_id FROM khotel_hotel_facility hf")
                    .append(" WHERE hf.facility_id IN (").append(String.join(", ", placeholders)).append(")")
                    .append(" GROUP BY hf.hotel_id HAVING COUNT(DISTINCT hf.facility_id) = :facilityCount)");
            binds.put("facilityCount", facilities.size());
        }

        if (c.checkIn() != null && c.checkOut() != null) {
            // ≥1 room with capacity >= guests AND a free unit across the range:
            //   total_units - SUM(overlapping non-cancelled units) >= 1
            where.append(" AND id IN (SELECT r.hotel_id FROM khotel_room r")
                    .append(" WHERE r.capacity >= :guests")
                    .append(" AND r.total_units - COALESCE((SELECT SUM(b.units) FROM khotel_booking b")
                    .append(" WHERE b.room_id = r.id AND b.status <> 'CANCELLED'")
                    .append(" AND b.check_in < :checkOut AND b.check_out > :checkIn), 0) >= 1)");
            binds.put("guests", Math.max(1, c.guests()));
            binds.put("checkIn", c.checkIn());
            binds.put("checkOut", c.checkOut());
        }

        return new Filter(where.toString(), binds);
    }

    private static DatabaseClient.GenericExecuteSpec bindAll(DatabaseClient.GenericExecuteSpec spec,
                                                             Map<String, Object> binds) {
        for (Map.Entry<String, Object> e : binds.entrySet()) {
            spec = spec.bind(e.getKey(), e.getValue());
        }
        return spec;
    }

    private record Filter(String where, Map<String, Object> binds) {
    }

    @Override
    public Flux<Hotel> findByOwner(String userId) {
        return hotelRepository.findByOwnerUserId(userId)
                .map(entity -> modelMapper.map(entity, Hotel.class))
                .doOnError(e -> log.error("Error finding hotels owned by {}: {}", userId, e.getMessage()));
    }

    private static final String ADMIN_HOTELS_SQL = """
            SELECT h.id, h.name, h.description, h.address, h.mobile, h.email,
                   h.check_in_time, h.check_out_time, h.created_at,
                   (SELECT COUNT(*) FROM khotel_room r WHERE r.hotel_id = h.id)    AS room_types,
                   (SELECT COUNT(*) FROM khotel_booking b WHERE b.hotel_id = h.id) AS bookings
            FROM khotel_hotel h ORDER BY h.name
            """;

    private static final String ADMIN_OWNERS_SQL = """
            SELECT o.hotel_id, u.id AS user_id, u.name, u.email
            FROM khotel_hotel_owner o JOIN khotel_user u ON u.id = o.user_id
            """;

    @Override
    public Flux<AdminHotelView> findAllForAdmin() {
        // One query for hotels+counts, one for all owners → grouped in memory (no per-hotel N+1).
        Mono<Map<String, List<AdminHotelView.OwnerRef>>> ownersByHotel = db.sql(ADMIN_OWNERS_SQL)
                .map((row, md) -> Map.entry(row.get("hotel_id", String.class),
                        AdminHotelView.OwnerRef.builder()
                                .userId(row.get("user_id", String.class))
                                .name(row.get("name", String.class))
                                .email(row.get("email", String.class))
                                .build()))
                .all()
                .collectMultimap(Map.Entry::getKey, Map.Entry::getValue)
                .map(mm -> {
                    Map<String, List<AdminHotelView.OwnerRef>> out = new java.util.LinkedHashMap<>();
                    mm.forEach((k, v) -> out.put(k, new ArrayList<>(v)));
                    return out;
                });

        return ownersByHotel.flatMapMany(owners -> db.sql(ADMIN_HOTELS_SQL)
                .map((row, md) -> AdminHotelView.builder()
                        .id(row.get("id", String.class))
                        .name(row.get("name", String.class))
                        .description(row.get("description", String.class))
                        .address(row.get("address", String.class))
                        .mobile(row.get("mobile", String.class))
                        .email(row.get("email", String.class))
                        .checkInTime(row.get("check_in_time", java.time.LocalTime.class))
                        .checkOutTime(row.get("check_out_time", java.time.LocalTime.class))
                        .createdAt(row.get("created_at", java.time.LocalDateTime.class))
                        .roomTypes(nz(row.get("room_types", Long.class)))
                        .bookings(nz(row.get("bookings", Long.class)))
                        .owners(owners.getOrDefault(row.get("id", String.class), List.of()))
                        .build())
                .all())
                .doOnError(e -> log.error("Error building admin hotel list: {}", e.getMessage()));
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    @Override
    public Mono<Hotel> findById(String id) {
        log.info("Finding hotel id: {}", id);
        return hotelRepository.findById(id)
                .map(entity -> modelMapper.map(entity, Hotel.class))
                .doOnError(e -> log.error("Error finding hotel id {}: {}", id, e.getMessage()));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        log.info("Deleting hotel id: {}", id);
        return hotelRepository.deleteById(id)
                .doOnError(e -> log.error("Error deleting hotel id {}: {}", id, e.getMessage()));
    }

    @Override
    public Mono<Boolean> existsById(String id) {
        return hotelRepository.existsById(id);
    }
}
