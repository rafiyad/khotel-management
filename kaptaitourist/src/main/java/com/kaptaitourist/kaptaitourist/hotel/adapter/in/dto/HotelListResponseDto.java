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
    private int page;           // zero-based page index returned
    private int size;           // page size used
    private long totalRecords;  // total hotels matching the filter (across all pages)
    private int totalPages;
    private List<Hotel> hotelData;
}
