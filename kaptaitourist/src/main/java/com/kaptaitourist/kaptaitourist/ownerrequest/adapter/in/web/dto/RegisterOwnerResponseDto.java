package com.kaptaitourist.kaptaitourist.ownerrequest.adapter.in.web.dto;

import com.kaptaitourist.kaptaitourist.ownerrequest.domain.OwnerRequest;
import com.kaptaitourist.kaptaitourist.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterOwnerResponseDto {
    private String message;
    private User userData;              // the created USER (password hash stripped)
    private OwnerRequest requestData;   // the filed PENDING request
}
