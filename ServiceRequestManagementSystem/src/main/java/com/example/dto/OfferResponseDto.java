package com.example.dto;

import com.example.entity.RequestStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OfferResponseDto(
        Long id,
        Long serviceRequestId,
        RequestStatus serviceRequestStatus,
        String description,
        LocalDateTime createdAt,
        LocalDateTime validUntil,
        BigDecimal finalPrice,
        PriceBreakdownDto priceBreakdown
) {

    public record PriceBreakdownDto(
            BigDecimal basePrice,
            BigDecimal executionModeAdditionalPrice,
            BigDecimal prioritySurcharge
    ) {
    }
}
