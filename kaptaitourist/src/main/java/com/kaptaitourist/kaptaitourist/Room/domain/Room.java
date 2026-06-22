package com.kaptaitourist.kaptaitourist.Room.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String roomType;
    private String description;
    private double pricePerNight;
    private boolean isAvailable;
    private List<String> imageUrls;
    private String hotelId;
    private String hotelName;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private boolean isDeleted;
}
