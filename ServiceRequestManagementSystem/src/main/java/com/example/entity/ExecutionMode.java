package com.example.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum ExecutionMode {
    REMOTE(BigDecimal.ZERO),
    ON_SITE(new BigDecimal("30"));

    private final BigDecimal additionalPrice;
}
