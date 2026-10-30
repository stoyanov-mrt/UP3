package com.example.entity;

/**
 * Thrown when the requested schedule violates a scheduling rule. The client can fix it by choosing other times.
 */
public class InvalidScheduleException extends RuntimeException {

    public InvalidScheduleException(String message) {
        super(message);
    }
}
