package com.kaptaitourist.kaptaitourist.facility.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FacilityAssignmentRequestDto {
    private String facilityId;
    private Boolean isComplimentary;     // defaults to true
    private BigDecimal additionalCharge; // optional, when not complimentary
    private String notes;
    private String createdBy;
}
