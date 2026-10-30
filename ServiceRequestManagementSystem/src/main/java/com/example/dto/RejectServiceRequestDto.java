package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectServiceRequestDto(
        @NotBlank @Size(max = 255) String reason
) {
}
