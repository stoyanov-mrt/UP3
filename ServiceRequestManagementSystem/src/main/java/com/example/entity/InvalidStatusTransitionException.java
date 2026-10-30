package com.example.entity;

/**
 * Thrown when a lifecycle operation is not allowed for the current status of a service request.
 */
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(String operation, RequestStatus currentStatus) {
        super("Cannot " + operation + " a service request in status " + currentStatus);
    }
}
