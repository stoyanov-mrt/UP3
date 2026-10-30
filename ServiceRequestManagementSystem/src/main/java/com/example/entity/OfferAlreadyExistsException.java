package com.example.entity;

public class OfferAlreadyExistsException extends RuntimeException {

    public OfferAlreadyExistsException() {
        super("The service request already has an offer");
    }
}
