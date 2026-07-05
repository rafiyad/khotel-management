package com.kaptaitourist.kaptaitourist.dashboard.adapter.out.persistence;

import com.kaptaitourist.kaptaitourist.dashboard.application.port.out.DashboardPort;
import com.kaptaitourist.kaptaitourist.dashboard.domain.DashboardSnapshot;
import com.kaptaitourist.kaptaitourist.dashboard.domain.PlatformSnapshot;
import io.r2dbc.spi.Row;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Computes dashboard aggregates with a few grouped SQL queries (no N+1) via DatabaseClient,
 * so no entity/repository coupling is needed for cross-table rollups.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DashboardAdapter implements DashboardPort {

    private final DatabaseClient db;

    private static final String ROOMS_SQL = """
            SELECT COUNT(*) AS room_types, COALESCE(SUM(total_units), 0) AS total_units
            FROM khotel_room WHERE hotel_id = :hotelId
            """;

    // All booking-derived figures in one pass over the hotel's bookings.
    private static final String BOOKINGS_SQL = """
            SELECT
              COALESCE(SUM(units) FILTER (WHERE status <> 'CANCELLED'
                        AND check_in <= :today AND check_out > :today), 0)          AS booked_tonight,
              COUNT(*) FILTER (WHERE status = 'CONFIRMED' AND check_out >= :today)  AS upcoming,
              COUNT(*) FILTER (WHERE status = 'CONFIRMED' AND check_in  = :today)   AS arrivals_today,
              COUNT(*) FILTER (WHERE status = 'CONFIRMED' AND check_out = :today)   AS departures_today,
              COUNT(*) FILTER (WHERE status = 'CANCELLED')                          AS cancelled,
              COALESCE(SUM(total_price) FILTER (WHERE status = 'CONFIRMED'
                        AND check_out >= :today), 0)                                AS upcoming_revenue
            FROM khotel_booking WHERE hotel_id = :hotelId
            """;

    private static final String FACILITIES_SQL = """
            SELECT COUNT(*) AS assigned,
                   COUNT(*) FILTER (WHERE hf.is_available = FALSE OR f.is_active = FALSE) AS unavailable
            FROM khotel_hotel_facility hf
            JOIN khotel_facility f ON f.id = hf.facility_id
            WHERE hf.hotel_id = :hotelId
            """;

    private static final String IMAGES_SQL = """
            SELECT COUNT(*) AS hotel_images
            FROM khotel_attachment WHERE hotel_id = :hotelId AND room_id IS NULL
            """;

    @Override
    public Mono<DashboardSnapshot> load(String hotelId, LocalDate today) {
        Mono<long[]> rooms = db.sql(ROOMS_SQL).bind("hotelId", hotelId)
                .map((row, md) -> new long[]{lng(row, "room_types"), lng(row, "total_units")})
                .one();

        Mono<BookingAgg> bookings = db.sql(BOOKINGS_SQL)
                .bind("hotelId", hotelId).bind("today", today)
                .map((row, md) -> new BookingAgg(
                        lng(row, "booked_tonight"), lng(row, "upcoming"), lng(row, "arrivals_today"),
                        lng(row, "departures_today"), lng(row, "cancelled"), dec(row, "upcoming_revenue")))
                .one();

        Mono<long[]> facilities = db.sql(FACILITIES_SQL).bind("hotelId", hotelId)
                .map((row, md) -> new long[]{lng(row, "assigned"), lng(row, "unavailable")})
                .one();

        Mono<Long> images = db.sql(IMAGES_SQL).bind("hotelId", hotelId)
                .map((row, md) -> lng(row, "hotel_images"))
                .one();

        return Mono.zip(rooms, bookings, facilities, images)
                .map(t -> {
                    long totalUnits = t.getT1()[1];
                    long bookedTonight = t.getT2().bookedTonight();
                    long available = Math.max(0, totalUnits - bookedTonight);
                    double occupancy = totalUnits > 0
                            ? Math.round(bookedTonight * 1000.0 / totalUnits) / 10.0
                            : 0.0;
                    return DashboardSnapshot.builder()
                            .roomTypes(t.getT1()[0])
                            .totalUnits(totalUnits)
                            .bookedTonight(bookedTonight)
                            .availableTonight(available)
                            .occupancyPct(occupancy)
                            .upcomingBookings(t.getT2().upcoming())
                            .arrivalsToday(t.getT2().arrivalsToday())
                            .departuresToday(t.getT2().departuresToday())
                            .cancelledBookings(t.getT2().cancelled())
                            .upcomingRevenue(t.getT2().upcomingRevenue())
                            .facilitiesAssigned(t.getT3()[0])
                            .facilitiesUnavailable(t.getT3()[1])
                            .hotelImages(t.getT4())
                            .build();
                })
                .doOnError(e -> log.error("Error building dashboard for hotel {}: {}", hotelId, e.getMessage()));
    }

    // ───────────────────────────── Platform-wide (admin) ─────────────────────────────

    private static final String PLATFORM_COUNTS_SQL = """
            SELECT (SELECT COUNT(*) FROM khotel_hotel)                        AS hotels,
                   (SELECT COUNT(DISTINCT user_id) FROM khotel_hotel_owner)   AS owners,
                   (SELECT COUNT(*) FROM khotel_user)                         AS users,
                   (SELECT COUNT(*) FROM khotel_facility)                     AS facilities
            """;

    private static final String PLATFORM_ROOMS_SQL = """
            SELECT COUNT(*) AS room_types, COALESCE(SUM(total_units), 0) AS total_units FROM khotel_room
            """;

    private static final String PLATFORM_BOOKINGS_SQL = """
            SELECT
              COALESCE(SUM(units) FILTER (WHERE status <> 'CANCELLED'
                        AND check_in <= :today AND check_out > :today), 0)          AS booked_tonight,
              COUNT(*) FILTER (WHERE status = 'CONFIRMED' AND check_out >= :today)  AS upcoming,
              COUNT(*) FILTER (WHERE status = 'CONFIRMED' AND check_in  = :today)   AS arrivals_today,
              COUNT(*) FILTER (WHERE status = 'CONFIRMED' AND check_out = :today)   AS departures_today,
              COUNT(*) FILTER (WHERE status = 'CANCELLED')                          AS cancelled,
              COALESCE(SUM(total_price) FILTER (WHERE status = 'CONFIRMED'
                        AND check_out >= :today), 0)                                AS upcoming_revenue
            FROM khotel_booking
            """;

    @Override
    public Mono<PlatformSnapshot> loadPlatform(LocalDate today) {
        Mono<long[]> counts = db.sql(PLATFORM_COUNTS_SQL)
                .map((row, md) -> new long[]{lng(row, "hotels"), lng(row, "owners"),
                        lng(row, "users"), lng(row, "facilities")})
                .one();

        Mono<long[]> rooms = db.sql(PLATFORM_ROOMS_SQL)
                .map((row, md) -> new long[]{lng(row, "room_types"), lng(row, "total_units")})
                .one();

        Mono<BookingAgg> bookings = db.sql(PLATFORM_BOOKINGS_SQL).bind("today", today)
                .map((row, md) -> new BookingAgg(
                        lng(row, "booked_tonight"), lng(row, "upcoming"), lng(row, "arrivals_today"),
                        lng(row, "departures_today"), lng(row, "cancelled"), dec(row, "upcoming_revenue")))
                .one();

        return Mono.zip(counts, rooms, bookings)
                .map(t -> {
                    long totalUnits = t.getT2()[1];
                    long bookedTonight = t.getT3().bookedTonight();
                    long available = Math.max(0, totalUnits - bookedTonight);
                    double occupancy = totalUnits > 0
                            ? Math.round(bookedTonight * 1000.0 / totalUnits) / 10.0
                            : 0.0;
                    return PlatformSnapshot.builder()
                            .hotels(t.getT1()[0])
                            .owners(t.getT1()[1])
                            .users(t.getT1()[2])
                            .facilities(t.getT1()[3])
                            .roomTypes(t.getT2()[0])
                            .totalUnits(totalUnits)
                            .bookedTonight(bookedTonight)
                            .availableTonight(available)
                            .occupancyPct(occupancy)
                            .upcomingBookings(t.getT3().upcoming())
                            .arrivalsToday(t.getT3().arrivalsToday())
                            .departuresToday(t.getT3().departuresToday())
                            .cancelledBookings(t.getT3().cancelled())
                            .upcomingRevenue(t.getT3().upcomingRevenue())
                            .build();
                })
                .doOnError(e -> log.error("Error building platform dashboard: {}", e.getMessage()));
    }

    private static long lng(Row row, String col) {
        Long v = row.get(col, Long.class);
        return v == null ? 0L : v;
    }

    private static BigDecimal dec(Row row, String col) {
        BigDecimal v = row.get(col, BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }

    private record BookingAgg(long bookedTonight, long upcoming, long arrivalsToday,
                              long departuresToday, long cancelled, BigDecimal upcomingRevenue) {
    }
}
