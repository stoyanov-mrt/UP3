package com.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * State is changed only through the constructor and the lifecycle operations - there are no setters,
 * so status, submittedAt and rejectionReason cannot bypass the lifecycle rules.
 */
@Entity
@Table(name = "service_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // required by JPA
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private CustomerInfo customerInfo;

    @Column(nullable = false, length = 2000)
    private String description;

    private String address;

    private String urgencyReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionMode executionMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    private String rejectionReason;

    /**
     * Creates a newly submitted request. status and submittedAt are system-managed.
     */
    public ServiceRequest(CustomerInfo customerInfo, String description, ServiceType serviceType,
                          ExecutionMode executionMode, Priority priority, String address, String urgencyReason) {
        this.customerInfo = customerInfo;
        this.description = description;
        this.serviceType = serviceType;
        this.executionMode = executionMode;
        this.priority = priority;
        this.address = address;
        this.urgencyReason = urgencyReason;
        this.status = RequestStatus.SUBMITTED;
        // Truncated to the precision stored by the database, so POST and GET return the same value
        this.submittedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }

    public void startProcessing() {
        if (status != RequestStatus.SUBMITTED) {
            throw new InvalidStatusTransitionException("start processing", status);
        }
        status = RequestStatus.PROCESSING;
    }

    public void reject(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        if (status != RequestStatus.PROCESSING) {
            throw new InvalidStatusTransitionException("reject", status);
        }
        status = RequestStatus.REJECTED;
        rejectionReason = reason;
    }
}
