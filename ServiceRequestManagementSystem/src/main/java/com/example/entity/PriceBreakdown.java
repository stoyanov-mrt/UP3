package com.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Immutable value object with the components of an offer's price in EUR.
 * Final price = (base price + execution mode additional price + priority surcharge) - loyalty discount;
 * the loyalty discount is calculated from the sum after all surcharges.
 */
@Embeddable
public record PriceBreakdown(
        @Column(nullable = false, precision = 10, scale = 2)
        BigDecimal basePrice,

        @Column(nullable = false, precision = 10, scale = 2)
        BigDecimal executionModeAdditionalPrice,

        @Column(nullable = false, precision = 10, scale = 2)
        BigDecimal prioritySurcharge,

        @Column(nullable = false, precision = 10, scale = 2)
        BigDecimal loyaltyDiscount
) {

    static final BigDecimal LOYALTY_DISCOUNT_RATE = new BigDecimal("0.10");

    public static PriceBreakdown of(ServiceType serviceType, ExecutionMode executionMode, Priority priority,
                                    boolean loyalCustomer) {
        BigDecimal base = money(serviceType.getBasePrice());
        BigDecimal additional = money(executionMode.getAdditionalPrice());
        BigDecimal surcharge = money(priority.getSurcharge());
        BigDecimal discount = loyalCustomer
                ? money(base.add(additional).add(surcharge).multiply(LOYALTY_DISCOUNT_RATE))
                : money(BigDecimal.ZERO);
        return new PriceBreakdown(base, additional, surcharge, discount);
    }

    // Every amount has 2 decimals, as stored in the database, so a new and a loaded offer look the same
    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal total() {
        return basePrice.add(executionModeAdditionalPrice).add(prioritySurcharge).subtract(loyaltyDiscount);
    }
}
