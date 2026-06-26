package com.kaptaitourist.kaptaitourist.booking.adapter.in.web.dto;

import com.kaptaitourist.kaptaitourist.booking.domain.Booking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingListResponseDto {
    private String message;
    private int totalRecords;
    private List<Booking> bookingData;
}
