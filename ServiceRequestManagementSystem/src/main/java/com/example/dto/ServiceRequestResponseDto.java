package com.example.dto;

import com.example.entity.ExecutionMode;
import com.example.entity.Priority;
import com.example.entity.RequestStatus;
import com.example.entity.ServiceType;

import java.time.LocalDateTime;

public record ServiceRequestResponseDto(
        Long id,
        String customerName,
        String customerEmail,
        String description,
        ServiceType serviceType,
        ExecutionMode executionMode,
        Priority priority,
        String address,
        String urgencyReason,
        RequestStatus status,
        LocalDateTime submittedAt,
        String rejectionReason
) {
}
