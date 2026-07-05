package com.kaptaitourist.kaptaitourist.dashboard.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Flat aggregate snapshot for a single hotel, computed from live data (nothing stored). */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardSnapshot {
    // rooms
    private long roomTypes;
    private long totalUnits;
    private long bookedTonight;
    private long availableTonight;
    private double occupancyPct;
    // bookings
    private long upcomingBookings;
    private long arrivalsToday;
    private long departuresToday;
    private long cancelledBookings;
    // revenue
    private BigDecimal upcomingRevenue;
    // facilities
    private long facilitiesAssigned;
    private long facilitiesUnavailable;
    // media
    private long hotelImages;
}
