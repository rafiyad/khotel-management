package com.kaptaitourist.kaptaitourist.ownerrequest.adapter.in.web.dto;

import com.kaptaitourist.kaptaitourist.ownerrequest.domain.OwnerRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OwnerRequestResponseDto {
    private String message;
    private OwnerRequest requestData;
}
