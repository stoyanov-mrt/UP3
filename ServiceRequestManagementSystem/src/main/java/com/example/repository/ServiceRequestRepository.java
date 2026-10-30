package com.example.repository;

import com.example.entity.RequestStatus;
import com.example.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    long countByCustomerInfoCustomerEmailIgnoreCaseAndStatus(String customerEmail, RequestStatus status);
}
