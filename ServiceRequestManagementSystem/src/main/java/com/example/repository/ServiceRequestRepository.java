package com.example.repository;

import com.example.entity.RequestStatus;
import com.example.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    long countByCustomerInfoCustomerEmailIgnoreCaseAndStatus(String customerEmail, RequestStatus status);

    List<ServiceRequest> findByStatusAndOfferValidUntilBefore(RequestStatus status, LocalDateTime moment);
}
