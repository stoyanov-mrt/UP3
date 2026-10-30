package com.example.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", unique = true)
    private Offer offer;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", unique = true)
    private ServiceExecution execution;

    /**
     * Creates a newly submitted request. status and submittedAt are system-managed;
     * submittedAt is the current time supplied by the service.
     */
    public ServiceRequest(CustomerInfo customerInfo, String description, ServiceType serviceType,
                          ExecutionMode executionMode, Priority priority, String address, String urgencyReason,
                          LocalDateTime submittedAt) {
        this.customerInfo = customerInfo;
        this.description = description;
        this.serviceType = serviceType;
        this.executionMode = executionMode;
        this.priority = priority;
        this.address = address;
        this.urgencyReason = urgencyReason;
        this.status = RequestStatus.SUBMITTED;
        this.submittedAt = submittedAt;
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

    /**
     * Creates the offer with a price calculated from the request's characteristics.
     * The status becomes OFFERED only after the offer has been created successfully.
     */
    public Offer createOffer(String description, LocalDateTime now) {
        if (offer != null) {
            throw new OfferAlreadyExistsException();
        }
        if (status != RequestStatus.PROCESSING) {
            throw new InvalidStatusTransitionException("create an offer for", status);
        }
        offer = new Offer(description, PriceBreakdown.of(serviceType, executionMode, priority), now);
        status = RequestStatus.OFFERED;
        return offer;
    }

    public void acceptOffer(LocalDateTime now) {
        requireValidOffer("accept the offer of", now);
        status = RequestStatus.ACCEPTED;
    }

    public void declineOffer(LocalDateTime now) {
        requireValidOffer("decline the offer of", now);
        status = RequestStatus.REJECTED;
    }

    public void schedule(LocalDateTime expectedStart, LocalDateTime expectedEnd, LocalDateTime now) {
        if (status != RequestStatus.ACCEPTED) {
            throw new InvalidStatusTransitionException("schedule", status);
        }
        if (!expectedEnd.isAfter(expectedStart)) {
            throw new InvalidScheduleException("expectedEnd must be after expectedStart");
        }
        LocalDateTime earliestStart = earliestStart(now);
        if (expectedStart.isBefore(earliestStart)) {
            throw new InvalidScheduleException(
                    executionMode + " service can start at the earliest at " + earliestStart);
        }
        execution = new ServiceExecution(expectedStart, expectedEnd);
        status = RequestStatus.SCHEDULED;
    }

    // REMOTE: at least 4 hours after scheduling; ON_SITE: at the earliest on the next working day
    private LocalDateTime earliestStart(LocalDateTime now) {
        return switch (executionMode) {
            case REMOTE -> now.plusHours(4);
            case ON_SITE -> WorkingDays.nextWorkingDay(now.toLocalDate()).atStartOfDay();
        };
    }

    /**
     * An expired offer can be neither accepted nor declined - the request becomes EXPIRED instead.
     */
    private void requireValidOffer(String operation, LocalDateTime now) {
        if (status != RequestStatus.OFFERED) {
            throw new InvalidStatusTransitionException(operation, status);
        }
        if (offer.isExpiredAt(now)) {
            status = RequestStatus.EXPIRED;
            throw new OfferExpiredException(offer.getValidUntil());
        }
    }
}
