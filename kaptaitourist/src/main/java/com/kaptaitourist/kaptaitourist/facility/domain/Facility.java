package com.kaptaitourist.kaptaitourist.facility.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Facility {
    private String id;
    private String name;
    private String category;
    private String icon;
    private String description;
    private String appliesTo;   // HOTEL | ROOM | BOTH
    private Boolean isActive;
    private Long version;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
