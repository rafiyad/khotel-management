package com.kaptaitourist.kaptaitourist.core.rolepermission.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Permission {
    private String id;
    private String permissionName;
    private String url;
    private String method;
    private Boolean requiresOwnership;
    private LocalDateTime createdAt;
    private String topMenuId;
    private String leftMenuId;

    @Override
    public String toString() {
        return "Permission{" +
                "id='" + id + '\'' +
                ", permissionName='" + permissionName + '\'' +
                ", url='" + url + '\'' +
                ", method='" + method + '\'' +
                ", createdAt=" + createdAt +
                ", topMenuId='" + topMenuId + '\'' +
                ", leftMenuId='" + leftMenuId + '\'' +
                '}';
    }

}
