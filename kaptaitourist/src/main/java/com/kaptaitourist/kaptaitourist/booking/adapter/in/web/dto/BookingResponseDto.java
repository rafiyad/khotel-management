package com.kaptaitourist.kaptaitourist.booking.adapter.in.web.dto;

import com.kaptaitourist.kaptaitourist.booking.domain.Booking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingResponseDto {
    private String message;
    private Booking bookingData;
}
