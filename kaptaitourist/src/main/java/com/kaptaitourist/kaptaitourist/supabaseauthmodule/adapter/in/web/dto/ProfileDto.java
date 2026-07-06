package com.kaptaitourist.kaptaitourist.supabaseauthmodule.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** A user's own profile view — email and mobile are masked. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileDto {
    private String id;
    private String name;
    private String email;    // masked, e.g. "***iyad@example.com"
    private String mobile;   // masked, e.g. "**********0000"
    private String gender;
    private Boolean isActive;
    private List<String> roles;
}
