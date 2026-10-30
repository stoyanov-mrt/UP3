package com.example.service;

import com.example.dto.CreateServiceRequestDto;
import com.example.dto.ServiceRequestResponseDto;
import com.example.entity.CustomerInfo;
import com.example.entity.ServiceRequest;
import com.example.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {

    private final ServiceRequestRepository repository;

    @Transactional
    public ServiceRequestResponseDto create(CreateServiceRequestDto dto) {
        ServiceRequest request = new ServiceRequest(
                new CustomerInfo(dto.customerName(), dto.customerEmail()),
                dto.description(),
                dto.serviceType(),
                dto.executionMode(),
                dto.priority(),
                dto.address(),
                dto.urgencyReason());
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
                request.getRejectionReason()
        );
    }
}
