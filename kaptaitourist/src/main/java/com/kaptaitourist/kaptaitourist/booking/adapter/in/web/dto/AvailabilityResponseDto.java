package com.kaptaitourist.kaptaitourist.booking.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AvailabilityResponseDto {
    private String roomId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int totalUnits;
    private long bookedUnits;
    private long availableUnits;
    private int requestedUnits;
    private boolean available;     // true when availableUnits >= requestedUnits
}
