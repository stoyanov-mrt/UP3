package com.example.controller;

import com.example.dto.CreateOfferDto;
import com.example.dto.CreateServiceRequestDto;
import com.example.dto.OfferResponseDto;
import com.example.dto.RejectServiceRequestDto;
import com.example.dto.ServiceRequestResponseDto;
import com.example.service.ServiceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class ServiceRequestController {

    private final ServiceRequestService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceRequestResponseDto create(@Valid @RequestBody CreateServiceRequestDto dto) {
        return service.create(dto);
    }

    @GetMapping
    public List<ServiceRequestResponseDto> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ServiceRequestResponseDto findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping("/{id}/start-processing")
    public ServiceRequestResponseDto startProcessing(@PathVariable Long id) {
        return service.startProcessing(id);
    }

    @PostMapping("/{id}/reject")
    public ServiceRequestResponseDto reject(@PathVariable Long id, @Valid @RequestBody RejectServiceRequestDto dto) {
        return service.reject(id, dto.reason());
    }

    @PostMapping("/{id}/offer")
    @ResponseStatus(HttpStatus.CREATED)
    public OfferResponseDto createOffer(@PathVariable Long id, @Valid @RequestBody CreateOfferDto dto) {
        return service.createOffer(id, dto.description());
    }

    @PostMapping("/{id}/offer/accept")
    public ServiceRequestResponseDto acceptOffer(@PathVariable Long id) {
        return service.acceptOffer(id);
    }

    @PostMapping("/{id}/offer/decline")
    public ServiceRequestResponseDto declineOffer(@PathVariable Long id) {
        return service.declineOffer(id);
    }
}
