package com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto;

import com.kaptaitourist.kaptaitourist.hotel.domain.Hotel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HotelResponseDto {
    private String message;
    private Hotel hotelData;
}
