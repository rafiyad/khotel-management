package com.kaptaitourist.kaptaitourist.facility.adapter.in.web.dto;

import com.kaptaitourist.kaptaitourist.facility.domain.Facility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FacilityResponseDto {
    private String message;
    private Facility facilityData;
}
