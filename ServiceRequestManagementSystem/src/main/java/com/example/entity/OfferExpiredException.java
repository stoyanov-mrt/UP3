package com.example.entity;

import java.time.LocalDateTime;

/**
 * Thrown when the customer tries to accept or decline an offer after its validUntil.
 * The request has already been moved to EXPIRED when this is thrown.
 */
public class OfferExpiredException extends RuntimeException {

    public OfferExpiredException(LocalDateTime validUntil) {
        super("The offer expired at " + validUntil + "; the service request is now EXPIRED");
    }
}
