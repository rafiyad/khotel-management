package com.kaptaitourist.kaptaitourist.user.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private String id;
    private String name;
    private String email;
    private String mobile;

    /** Never serialized to clients. */
    @JsonIgnore
    private String passwordHash;

    private Boolean isActive;
    private List<String> roles;   // role names, e.g. ["USER"]
    private Long version;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
