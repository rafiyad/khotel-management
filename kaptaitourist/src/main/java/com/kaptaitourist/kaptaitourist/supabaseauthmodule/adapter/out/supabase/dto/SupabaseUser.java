package com.kaptaitourist.kaptaitourist.supabaseauthmodule.adapter.out.supabase.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** The Supabase GoTrue user object (only the fields we consume). */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupabaseUser {
    private String id;
    private String email;

    @JsonProperty("user_metadata")
    private Map<String, Object> userMetadata;
}
