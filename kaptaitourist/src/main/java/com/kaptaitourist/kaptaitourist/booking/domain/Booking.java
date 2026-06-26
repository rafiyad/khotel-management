package com.kaptaitourist.kaptaitourist.booking.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Booking {
    private String id;
    private String hotelId;
    private String roomId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int units;
    private String guestName;
    private String guestPhone;
    private String guestEmail;
    private Integer numberOfGuests;
    private String status;        // CONFIRMED | CANCELLED
    private BigDecimal totalPrice;
    private Long version;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
