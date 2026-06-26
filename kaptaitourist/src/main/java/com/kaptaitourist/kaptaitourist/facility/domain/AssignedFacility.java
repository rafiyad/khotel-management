package com.kaptaitourist.kaptaitourist.facility.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A facility as offered by a specific hotel or room — the catalog facility plus
 * the per-offering link attributes (free/paid, charge, notes).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AssignedFacility {
    private Facility facility;
    private Boolean isComplimentary;
    private BigDecimal additionalCharge;
    private String notes;
}
