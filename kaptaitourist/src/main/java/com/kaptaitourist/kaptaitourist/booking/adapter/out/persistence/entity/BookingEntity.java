package com.kaptaitourist.kaptaitourist.booking.adapter.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table("khotel_booking")
public class BookingEntity implements Persistable<String> {
    @Id
    private String id;
    private String hotelId;
    private String roomId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int units;
    private String guestName;
    private String guestPhone;
    private String guestEmail;
    private Integer numberOfGuests;
    private String status;
    private BigDecimal totalPrice;
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
