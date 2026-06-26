package com.kaptaitourist.kaptaitourist.facility.adapter.in.web.dto;

import com.kaptaitourist.kaptaitourist.facility.domain.AssignedFacility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AssignedFacilityListResponseDto {
    private String message;
    private int totalRecords;
    private List<AssignedFacility> data;
}
