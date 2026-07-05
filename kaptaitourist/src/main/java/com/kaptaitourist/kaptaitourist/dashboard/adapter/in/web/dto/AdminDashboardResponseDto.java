package com.kaptaitourist.kaptaitourist.dashboard.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Platform-wide dashboard for admins: totals across all hotels. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminDashboardResponseDto {
    private String message;
    private LocalDate asOf;
    private Platform platform;
    private Rooms rooms;
    private Bookings bookings;
    private Revenue revenue;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Platform {
        private long hotels;
        private long owners;   // distinct users who own at least one hotel
        private long users;
        private long facilities;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Rooms {
        private long roomTypes;
        private long totalUnits;
        private long bookedTonight;
        private long availableTonight;
        private double occupancyPct;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Bookings {
        private long upcoming;
        private long arrivalsToday;
        private long departuresToday;
        private long cancelled;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Revenue {
        private BigDecimal upcoming;
    }
}
