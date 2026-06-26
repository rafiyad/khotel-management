package com.kaptaitourist.kaptaitourist.user.adapter.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table("khotel_role")
public class RoleEntity {
    @Id
    private String id;
    private String name;
    private String description;
}
