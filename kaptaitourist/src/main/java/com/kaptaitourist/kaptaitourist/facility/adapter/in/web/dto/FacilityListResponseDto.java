package com.kaptaitourist.kaptaitourist.facility.adapter.in.web.dto;

import com.kaptaitourist.kaptaitourist.facility.domain.Facility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FacilityListResponseDto {
    private String message;
    private int totalRecords;
    private List<Facility> facilityData;
}
