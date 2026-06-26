package com.kaptaitourist.kaptaitourist.Room.domain;

import com.kaptaitourist.kaptaitourist.image.domain.Image;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Room {
    private String id;
    private String roomName;
    private int capacity;
    private int totalUnits;
    private String roomType;
    private boolean isAirConditioned;
    private String description;
    private BigDecimal pricePerNight;
    private BigDecimal discount;
    private boolean isAvailable;
    private String prerequisites;
    private String hotelId;
    private String hotelName;
    private Long version;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private List<Image> images;
}
