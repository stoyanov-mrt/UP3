package com.example.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum ServiceType {
    INSTALLATION(new BigDecimal("120")),
    REPAIR(new BigDecimal("100")),
    CONSULTATION(new BigDecimal("50"));

    private final BigDecimal basePrice;
}
