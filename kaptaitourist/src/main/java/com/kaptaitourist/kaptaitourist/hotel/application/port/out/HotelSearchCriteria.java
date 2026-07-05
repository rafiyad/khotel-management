package com.kaptaitourist.kaptaitourist.hotel.application.port.out;

import java.time.LocalDate;
import java.util.List;

/**
 * Filter for the paginated hotel list. All filters are optional and combine with AND:
 * - {@code search}: case-insensitive hotel-name substring (null/blank = all).
 * - {@code facilityIds}: keep only hotels that have ALL of these facilities at hotel level.
 * - {@code checkIn}/{@code checkOut}: keep only hotels with ≥1 room that has a free unit across
 *   the range and capacity ≥ {@code guests}. Both must be set for the date filter to apply.
 */
public record HotelSearchCriteria(
        String search,
        List<String> facilityIds,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        int size,
        long offset) {
}
