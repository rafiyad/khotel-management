package com.kaptaitourist.kaptaitourist.Room.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomRequestDto {
    private String roomName;
    private Integer capacity;
    private String roomType;
    private Boolean isAirConditioned;
    private String description;
    private String prerequisites;
    private BigDecimal pricePerNight;
    private BigDecimal discount;
    private Boolean isAvailable;
    private String createdBy;
    private String updatedBy;
}
