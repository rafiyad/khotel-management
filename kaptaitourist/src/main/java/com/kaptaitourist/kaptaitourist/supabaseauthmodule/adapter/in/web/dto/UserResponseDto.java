package com.kaptaitourist.kaptaitourist.supabaseauthmodule.adapter.in.web.dto;

import com.kaptaitourist.kaptaitourist.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDto {
    private String message;
    private User userData;
}
