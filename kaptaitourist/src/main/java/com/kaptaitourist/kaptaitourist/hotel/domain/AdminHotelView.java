package com.kaptaitourist.kaptaitourist.hotel.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Admin oversight view of a hotel: no image enrichment, but owners + room-type/booking counts. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminHotelView {
    private String id;
    private String name;
    private String description;
    private String address;
    private String mobile;
    private String email;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private LocalDateTime createdAt;
    private long roomTypes;
    private long bookings;
    private List<OwnerRef> owners;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OwnerRef {
        private String userId;
        private String name;
        private String email;
    }
}
