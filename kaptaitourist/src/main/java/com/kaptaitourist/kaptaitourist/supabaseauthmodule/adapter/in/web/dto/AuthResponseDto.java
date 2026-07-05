package com.kaptaitourist.kaptaitourist.supabaseauthmodule.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDto {
    private String token;
    private String tokenType;   // "Bearer"
    private String userId;
    private String email;
    private List<String> roles;
}
