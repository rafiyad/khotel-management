package com.kaptaitourist.kaptaitourist.dashboard.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Platform-wide aggregate snapshot (all hotels) for the admin dashboard. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlatformSnapshot {
    // platform
    private long hotels;
    private long owners;
    private long users;
    private long facilities;
    // rooms (all hotels)
    private long roomTypes;
    private long totalUnits;
    private long bookedTonight;
    private long availableTonight;
    private double occupancyPct;
    // bookings (all hotels)
    private long upcomingBookings;
    private long arrivalsToday;
    private long departuresToday;
    private long cancelledBookings;
    // revenue
    private BigDecimal upcomingRevenue;
}
