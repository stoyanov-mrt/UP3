package com.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Immutable value object with the customer's contact details for a single service request.
 * It has no identity of its own and is persisted as part of {@link ServiceRequest}.
 */
@Embeddable
public record CustomerInfo(
        @Column(name = "customer_name", nullable = false)
        String customerName,

        @Column(name = "customer_email", nullable = false)
        String customerEmail
) {
}
