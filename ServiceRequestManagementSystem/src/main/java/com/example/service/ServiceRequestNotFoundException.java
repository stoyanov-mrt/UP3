package com.example.service;

public class ServiceRequestNotFoundException extends RuntimeException {

    public ServiceRequestNotFoundException(Long id) {
        super("Service request " + id + " not found");
    }
}
