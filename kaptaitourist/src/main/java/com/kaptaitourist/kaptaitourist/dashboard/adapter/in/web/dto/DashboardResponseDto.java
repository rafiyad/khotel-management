package com.kaptaitourist.kaptaitourist.dashboard.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Owner/admin dashboard for one hotel: occupancy, bookings, revenue, facilities, media. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResponseDto {
    private String message;
    private String hotelId;
    private LocalDate asOf;      // the date the "tonight"/"today" figures are computed for
    private Rooms rooms;
    private Bookings bookings;
    private Revenue revenue;
    private Facilities facilities;
    private Media media;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Rooms {
        private long roomTypes;        // number of room-type rows
        private long totalUnits;       // sum of physical units across types
        private long bookedTonight;    // units occupied for tonight
        private long availableTonight; // totalUnits - bookedTonight (>= 0)
        private double occupancyPct;   // bookedTonight / totalUnits * 100, 1dp
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Bookings {
        private long upcoming;         // CONFIRMED with check_out >= today
        private long arrivalsToday;    // CONFIRMED with check_in == today
        private long departuresToday;  // CONFIRMED with check_out == today
        private long cancelled;        // CANCELLED (all time)
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Revenue {
        private BigDecimal upcoming;   // sum of total_price for upcoming CONFIRMED bookings
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Facilities {
        private long assigned;         // facilities assigned to the hotel
        private long unavailable;      // assigned but whose catalog entry is inactive
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Media {
        private long hotelImages;      // hotel-level images (room_id IS NULL)
    }
}
