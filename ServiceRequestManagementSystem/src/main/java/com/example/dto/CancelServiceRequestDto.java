package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelServiceRequestDto(
        @NotBlank @Size(max = 255) String reason
) {
}
