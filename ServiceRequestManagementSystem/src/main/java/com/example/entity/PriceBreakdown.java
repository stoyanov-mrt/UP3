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
        BigDecimal base = serviceType.getBasePrice();
        BigDecimal additional = executionMode.getAdditionalPrice();
        BigDecimal surcharge = priority.getSurcharge();
        BigDecimal discount = loyalCustomer
                ? base.add(additional).add(surcharge).multiply(LOYALTY_DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new PriceBreakdown(base, additional, surcharge, discount);
    }

    public BigDecimal total() {
        return basePrice.add(executionModeAdditionalPrice).add(prioritySurcharge).subtract(loyaltyDiscount);
    }
}
