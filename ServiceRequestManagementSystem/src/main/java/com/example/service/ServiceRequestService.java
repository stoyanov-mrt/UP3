package com.example.service;

import com.example.dto.CreateServiceRequestDto;
import com.example.dto.ServiceRequestResponseDto;
import com.example.entity.CustomerInfo;
import com.example.entity.RequestStatus;
import com.example.entity.ServiceRequest;
import com.example.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {

    private final ServiceRequestRepository repository;

    @Transactional
    public ServiceRequestResponseDto create(CreateServiceRequestDto dto) {
        ServiceRequest request = new ServiceRequest();
        request.setCustomerInfo(new CustomerInfo(dto.customerName(), dto.customerEmail()));
        request.setDescription(dto.description());
        request.setServiceType(dto.serviceType());
        request.setExecutionMode(dto.executionMode());
        request.setPriority(dto.priority());
        request.setAddress(dto.address());
        request.setUrgencyReason(dto.urgencyReason());
        // System-managed values
        request.setStatus(RequestStatus.SUBMITTED);
        // Truncated to the precision stored by the database, so POST and GET return the same value
        request.setSubmittedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
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
        return repository.findById(id)
                .map(this::toResponse)
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
                request.getSubmittedAt()
        );
    }
}
