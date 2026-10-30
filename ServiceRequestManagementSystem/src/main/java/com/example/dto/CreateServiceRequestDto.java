package com.example.dto;

import com.example.entity.ExecutionMode;
import com.example.entity.Priority;
import com.example.entity.ServiceType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Data provided by the customer. id, status and submittedAt are intentionally absent -
 * they are managed by the system.
 */
public record CreateServiceRequestDto(
        @NotBlank String customerName,
        @NotBlank @Email String customerEmail,
        @NotBlank String description,
        @NotNull ServiceType serviceType,
        @NotNull ExecutionMode executionMode,
        @NotNull Priority priority,
        String address,
        String urgencyReason
) {
}
