package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Only the description of the proposed work is provided - price, createdAt and validUntil are calculated.
 */
public record CreateOfferDto(
        @NotBlank @Size(max = 2000) String description
) {
}
