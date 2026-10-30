package com.example.service;

import com.example.dto.CreateServiceRequestDto;
import com.example.dto.OfferResponseDto;
import com.example.dto.ServiceRequestResponseDto;
import com.example.entity.CustomerInfo;
import com.example.entity.Offer;
import com.example.entity.OfferExpiredException;
import com.example.entity.PriceBreakdown;
import com.example.entity.RequestStatus;
import com.example.entity.ServiceExecution;
import com.example.entity.ServiceRequest;
import com.example.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {

    static final int LOYALTY_MIN_COMPLETED_REQUESTS = 3;

    private final ServiceRequestRepository repository;
    private final Clock clock;

    @Transactional
    public ServiceRequestResponseDto create(CreateServiceRequestDto dto) {
        ServiceRequest request = new ServiceRequest(
                new CustomerInfo(dto.customerName(), dto.customerEmail()),
                dto.description(),
                dto.serviceType(),
                dto.executionMode(),
                dto.priority(),
                dto.address(),
                dto.urgencyReason(),
                now());
        return toResponse(repository.save(request));
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponseDto> findAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceRequestResponseDto findById(Long id) {
        return toResponse(getRequest(id));
    }

    // The status change is flushed by JPA dirty checking when the transaction commits
    @Transactional
    public ServiceRequestResponseDto startProcessing(Long id) {
        ServiceRequest request = getRequest(id);
        request.startProcessing();
        return toResponse(request);
    }

    @Transactional
    public ServiceRequestResponseDto reject(Long id, String reason) {
        ServiceRequest request = getRequest(id);
        request.reject(reason);
        return toResponse(request);
    }

    @Transactional
    public OfferResponseDto createOffer(Long id, String description) {
        ServiceRequest request = getRequest(id);
        Offer offer = request.createOffer(description, isLoyalCustomer(request), now());
        // The request is already managed: flush cascades PERSIST to this same offer instance, so its
        // generated id is available for the response. save() would merge and persist a copy instead.
        repository.flush();
        return toResponse(request, offer);
    }

    // noRollbackFor: an expired offer moves the request to EXPIRED, which must be saved even though
    // the accept/decline operation itself fails with 409
    @Transactional(noRollbackFor = OfferExpiredException.class)
    public ServiceRequestResponseDto acceptOffer(Long id) {
        ServiceRequest request = getRequest(id);
        request.acceptOffer(now());
        return toResponse(request);
    }

    @Transactional(noRollbackFor = OfferExpiredException.class)
    public ServiceRequestResponseDto declineOffer(Long id) {
        ServiceRequest request = getRequest(id);
        request.declineOffer(now());
        return toResponse(request);
    }

    @Transactional
    public ServiceRequestResponseDto schedule(Long id, LocalDateTime expectedStart, LocalDateTime expectedEnd) {
        ServiceRequest request = getRequest(id);
        request.schedule(expectedStart, expectedEnd, now());
        return toResponse(request);
    }

    @Transactional
    public ServiceRequestResponseDto startExecution(Long id) {
        ServiceRequest request = getRequest(id);
        request.startExecution(now());
        return toResponse(request);
    }

    @Transactional
    public ServiceRequestResponseDto completeExecution(Long id, String result) {
        ServiceRequest request = getRequest(id);
        request.completeExecution(result, now());
        return toResponse(request);
    }

    @Transactional
    public ServiceRequestResponseDto cancel(Long id, String reason) {
        ServiceRequest request = getRequest(id);
        request.cancel(reason, now());
        return toResponse(request);
    }

    // A customer is identified by email (case-insensitive) - CustomerInfo has no identity of its own
    private boolean isLoyalCustomer(ServiceRequest request) {
        long completed = repository.countByCustomerInfoCustomerEmailIgnoreCaseAndStatus(
                request.getCustomerInfo().customerEmail(), RequestStatus.COMPLETED);
        return completed >= LOYALTY_MIN_COMPLETED_REQUESTS;
    }

    // Truncated to the precision stored by the database, so POST and GET return the same value
    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    private ServiceRequest getRequest(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ServiceRequestNotFoundException(id));
    }

    private ServiceRequestResponseDto toResponse(ServiceRequest request) {
        return new ServiceRequestResponseDto(
                request.getId(),
                request.getCustomerInfo().customerName(),
                request.getCustomerInfo().customerEmail(),
                request.getDescription(),
                request.getServiceType(),
                request.getExecutionMode(),
                request.getPriority(),
                request.getAddress(),
                request.getUrgencyReason(),
                request.getStatus(),
                request.getSubmittedAt(),
                request.getRejectionReason(),
                request.getCancellationReason(),
                toResponse(request.getExecution())
        );
    }

    private ServiceRequestResponseDto.ExecutionDto toResponse(ServiceExecution execution) {
        if (execution == null) {
            return null;
        }
        return new ServiceRequestResponseDto.ExecutionDto(
                execution.getExpectedStart(),
                execution.getExpectedEnd(),
                execution.getActualStart(),
                execution.getActualEnd(),
                execution.getResult()
        );
    }

    private OfferResponseDto toResponse(ServiceRequest request, Offer offer) {
        PriceBreakdown price = offer.getPriceBreakdown();
        return new OfferResponseDto(
                offer.getId(),
                request.getId(),
                request.getStatus(),
                offer.getDescription(),
                offer.getCreatedAt(),
                offer.getValidUntil(),
                offer.getFinalPrice(),
                new OfferResponseDto.PriceBreakdownDto(
                        price.basePrice(),
                        price.executionModeAdditionalPrice(),
                        price.prioritySurcharge(),
                        price.loyaltyDiscount())
        );
    }
}
