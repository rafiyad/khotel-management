package com.kaptaitourist.kaptaitourist.ownerrequest.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** An owner request joined with its requester's identity, for the admin review list. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OwnerRequestView {
    private String id;
    private String userId;
    private String requesterName;
    private String requesterEmail;
    private String hotelName;
    private String message;
    private String status;
    private String decidedBy;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;
}
