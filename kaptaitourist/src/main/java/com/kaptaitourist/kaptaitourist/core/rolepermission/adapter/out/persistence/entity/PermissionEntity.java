package com.kaptaitourist.kaptaitourist.core.rolepermission.adapter.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "permission")
public class PermissionEntity {
    private String id;
    private String permissionName;
    private String url;
    private String method;
    private LocalDateTime createdAt;


}
