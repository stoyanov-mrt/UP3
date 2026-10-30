package com.example.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ScheduleServiceRequestDto(
        @NotNull LocalDateTime expectedStart,
        @NotNull LocalDateTime expectedEnd
) {
}
