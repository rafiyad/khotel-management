package com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HotelRequestDto {
    private String name;
    private String description;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private String mobile;
    private String email;
    private String website;
    private String address;
    private String googleMapUrl;
    private String createdBy;
    private String updatedBy;
}
