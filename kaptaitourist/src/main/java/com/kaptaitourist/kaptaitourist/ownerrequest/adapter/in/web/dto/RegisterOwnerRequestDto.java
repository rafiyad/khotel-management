package com.kaptaitourist.kaptaitourist.ownerrequest.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterOwnerRequestDto {
    // user fields
    private String name;
    private String email;
    private String mobile;
    private String gender;
    private String password;
    // enlistment fields
    private String hotelName;
    private String message;
}
