package com.kaptaitourist.kaptaitourist.hotel.adapter.in.dto;

import com.kaptaitourist.kaptaitourist.hotel.domain.AdminHotelView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminHotelListResponseDto {
    private String message;
    private int totalRecords;
    private List<AdminHotelView> hotelData;
}
