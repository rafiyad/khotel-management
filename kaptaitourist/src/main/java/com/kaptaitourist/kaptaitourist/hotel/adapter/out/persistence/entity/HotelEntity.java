package com.kaptaitourist.kaptaitourist.hotel.adapter.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table("khotel_hotel")
public class HotelEntity implements Persistable<String> {
    @Id
    private String id;
    private String name;
    private String description;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private String mobile;
    private String email;
    private String website;
    private String address;
    private String googleMapUrl;
    @Version
    private Long version;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    @Override
    public boolean isNew() {
        boolean isNull = Objects.isNull(this.id);
        this.id = isNull ? UUID.randomUUID().toString() : this.id;
        return isNull;
    }
}
