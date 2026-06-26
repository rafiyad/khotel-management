package com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto;

import com.kaptaitourist.kaptaitourist.hotel.domain.Hotel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HotelListResponseDto {
    private String message;
    private int totalRecords;
    private List<Hotel> hotelData;
}
