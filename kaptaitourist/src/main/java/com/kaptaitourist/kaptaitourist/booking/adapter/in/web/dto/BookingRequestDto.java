package com.kaptaitourist.kaptaitourist.booking.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingRequestDto {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkIn;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkOut;
    private Integer units;            // defaults to 1
    private String guestName;
    private String guestPhone;
    private String guestEmail;
    private Integer numberOfGuests;
    private String createdBy;
}
