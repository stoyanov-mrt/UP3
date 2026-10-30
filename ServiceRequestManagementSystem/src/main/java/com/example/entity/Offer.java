package com.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Offer for a single service request. Created only through {@link ServiceRequest#createOffer(String)}.
 */
@Entity
@Table(name = "offers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // required by JPA
public class Offer {

    static final int VALIDITY_DAYS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String description;

    @Embedded
    private PriceBreakdown priceBreakdown;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime validUntil;

    Offer(String description, PriceBreakdown priceBreakdown, LocalDateTime createdAt) {
        if (priceBreakdown.total().signum() <= 0) {
            throw new IllegalArgumentException("The final price must be positive");
        }
        this.description = description;
        this.priceBreakdown = priceBreakdown;
        this.createdAt = createdAt;
        this.validUntil = createdAt.plusDays(VALIDITY_DAYS);
    }

    public BigDecimal getFinalPrice() {
        return priceBreakdown.total();
    }
}
