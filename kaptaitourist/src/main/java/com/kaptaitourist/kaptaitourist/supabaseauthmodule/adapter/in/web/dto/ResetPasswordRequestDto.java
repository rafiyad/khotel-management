package com.kaptaitourist.kaptaitourist.supabaseauthmodule.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload for the OTP-code reset flow (§3.4 option A of doc/supabase-auth.md). */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordRequestDto {
    private String email;
    private String token;        // the one-time code from the recovery email
    private String newPassword;
}
