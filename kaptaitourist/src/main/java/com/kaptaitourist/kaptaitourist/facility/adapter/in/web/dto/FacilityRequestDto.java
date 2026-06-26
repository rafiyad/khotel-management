package com.kaptaitourist.kaptaitourist.facility.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FacilityRequestDto {
    private String name;
    private String category;
    private String icon;
    private String description;
    private String appliesTo;   // HOTEL | ROOM | BOTH (defaults to BOTH)
    private Boolean isActive;
    private String createdBy;
    private String updatedBy;
}
