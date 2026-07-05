package com.kaptaitourist.kaptaitourist.supabaseauthmodule.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Simple {"message": "..."} envelope for endpoints that return no data (forgot/reset). */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageResponse {
    private String message;
}
