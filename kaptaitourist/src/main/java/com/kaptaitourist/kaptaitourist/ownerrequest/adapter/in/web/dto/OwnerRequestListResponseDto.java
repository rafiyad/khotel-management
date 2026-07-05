package com.kaptaitourist.kaptaitourist.ownerrequest.adapter.in.web.dto;

import com.kaptaitourist.kaptaitourist.ownerrequest.domain.OwnerRequestView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OwnerRequestListResponseDto {
    private String message;
    private int totalRecords;
    private List<OwnerRequestView> requestData;
}
