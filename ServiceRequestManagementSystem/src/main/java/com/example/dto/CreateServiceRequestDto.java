package com.example.dto;

import com.example.dto.validation.ConditionalRequestFields;
import com.example.entity.ExecutionMode;
import com.example.entity.Priority;
import com.example.entity.ServiceType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Data provided by the customer. id, status and submittedAt are intentionally absent -
 * they are managed by the system.
 * Max sizes match the database column lengths, so too long input is a 400 and not a database error.
 */
@ConditionalRequestFields
public record CreateServiceRequestDto(
        @NotBlank @Size(max = 255) String customerName,
        @NotBlank @Email @Size(max = 255) String customerEmail,
        @NotBlank @Size(min = 10, max = 2000) String description,
        @NotNull ServiceType serviceType,
        @NotNull ExecutionMode executionMode,
        @NotNull Priority priority,
        @Size(max = 255) String address,
        @Size(max = 255) String urgencyReason
) {
}
