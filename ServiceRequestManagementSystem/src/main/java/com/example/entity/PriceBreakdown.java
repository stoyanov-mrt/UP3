package com.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

/**
 * Immutable value object with the components of an offer's price. The final price is their sum.
 */
@Embeddable
public record PriceBreakdown(
        @Column(nullable = false, precision = 10, scale = 2)
        BigDecimal basePrice,

        @Column(nullable = false, precision = 10, scale = 2)
        BigDecimal executionModeAdditionalPrice,

        @Column(nullable = false, precision = 10, scale = 2)
        BigDecimal prioritySurcharge
) {

    public static PriceBreakdown of(ServiceType serviceType, ExecutionMode executionMode, Priority priority) {
        return new PriceBreakdown(
                serviceType.getBasePrice(),
                executionMode.getAdditionalPrice(),
                priority.getSurcharge());
    }

    public BigDecimal total() {
        return basePrice.add(executionModeAdditionalPrice).add(prioritySurcharge);
    }
}
