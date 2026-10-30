package com.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Expires overdue offers periodically, so a request becomes EXPIRED even if nobody tries to accept or decline it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OfferExpirationJob {

    private final ServiceRequestService service;

    @Scheduled(initialDelayString = "${app.offer-expiration.interval:PT1M}",
            fixedDelayString = "${app.offer-expiration.interval:PT1M}")
    public void expireOverdueOffers() {
        int expired = service.expireOverdueOffers();
        if (expired > 0) {
            log.info("Expired {} service request(s) with overdue offers", expired);
        }
    }
}
