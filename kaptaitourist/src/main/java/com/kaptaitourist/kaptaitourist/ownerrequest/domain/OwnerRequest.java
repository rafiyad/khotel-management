package com.kaptaitourist.kaptaitourist.ownerrequest.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OwnerRequest {
    private String id;
    private String userId;
    private String hotelName;
    private String message;
    private String status;      // PENDING | APPROVED | REJECTED
    private String decidedBy;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
