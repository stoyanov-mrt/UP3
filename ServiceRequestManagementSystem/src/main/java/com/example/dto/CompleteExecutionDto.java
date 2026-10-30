package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteExecutionDto(
        @NotBlank @Size(max = 2000) String result
) {
}
