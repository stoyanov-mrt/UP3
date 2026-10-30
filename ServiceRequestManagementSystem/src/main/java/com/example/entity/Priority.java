package com.example.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum Priority {
    STANDARD(BigDecimal.ZERO),
    URGENT(new BigDecimal("40"));

    private final BigDecimal surcharge;
}
