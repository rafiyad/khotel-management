package com.kaptaitourist.kaptaitourist.user.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestDto {
    private String email;    // provide email OR mobile
    private String mobile;
    private String password;
}
