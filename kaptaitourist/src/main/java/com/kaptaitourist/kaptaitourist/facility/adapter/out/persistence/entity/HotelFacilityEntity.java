package com.kaptaitourist.kaptaitourist.facility.adapter.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table("khotel_hotel_facility")
public class HotelFacilityEntity implements Persistable<String> {
    @Id
    private String id;
    private String hotelId;
    private String facilityId;
    private Boolean isComplimentary;
    private BigDecimal additionalCharge;
    private String notes;
    private String createdBy;
    private LocalDateTime createdAt;

    @Override
    public boolean isNew() {
        boolean isNull = Objects.isNull(this.id);
        this.id = isNull ? UUID.randomUUID().toString() : this.id;
        return isNull;
    }
}
