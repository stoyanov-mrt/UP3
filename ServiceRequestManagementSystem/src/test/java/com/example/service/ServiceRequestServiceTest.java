package com.example.service;

import com.example.dto.OfferResponseDto;
import com.example.dto.ServiceRequestResponseDto;
import com.example.entity.CancellationDeadlinePassedException;
import com.example.entity.CustomerInfo;
import com.example.entity.ExecutionMode;
import com.example.entity.InvalidScheduleException;
import com.example.entity.InvalidStatusTransitionException;
import com.example.entity.OfferExpiredException;
import com.example.entity.Priority;
import com.example.entity.RequestStatus;
import com.example.entity.ServiceRequest;
import com.example.entity.ServiceType;
import com.example.repository.ServiceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests of ServiceRequestService. Only the repository is mocked (it needs a database); the domain objects
 * are real, and the clock is a real fixed clock, so time-dependent rules are deterministic.
 */
@ExtendWith(MockitoExtension.class)
class ServiceRequestServiceTest {

    private static final long ID = 1L;
    private static final String EMAIL = "ivan@example.com";
    // Friday
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 30, 10, 0);

    @Mock
    private ServiceRequestRepository repository;

    private ServiceRequestService service;

    @BeforeEach
    void setUp() {
        // The service is created directly with a real fixed clock - a Clock is a value, not a dependency to mock
        service = new ServiceRequestService(repository, Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    @Nested
    class CreateOffer {

        @Test
        void newCustomerPaysAllSurchargesWithoutDiscount() {
            ServiceRequest request = processing(ServiceType.INSTALLATION, ExecutionMode.ON_SITE, Priority.URGENT);
            givenRequest(request);
            givenCompletedRequests(0);

            OfferResponseDto offer = service.createOffer(ID, "Install boiler");

            assertEquals(new BigDecimal("120.00"), offer.priceBreakdown().basePrice());
            assertEquals(new BigDecimal("30.00"), offer.priceBreakdown().executionModeAdditionalPrice());
            assertEquals(new BigDecimal("40.00"), offer.priceBreakdown().prioritySurcharge());
            assertEquals(new BigDecimal("0.00"), offer.priceBreakdown().loyaltyDiscount());
            assertEquals(new BigDecimal("190.00"), offer.finalPrice());
            assertEquals(RequestStatus.OFFERED, request.getStatus());
        }

        @Test
        void offerIsValidForThreeCalendarDays() {
            givenRequest(processing(ServiceType.REPAIR, ExecutionMode.REMOTE, Priority.STANDARD));
            givenCompletedRequests(0);

            OfferResponseDto offer = service.createOffer(ID, "Repair");

            assertEquals(NOW, offer.createdAt());
            assertEquals(NOW.plusDays(3), offer.validUntil());
        }

        @Test
        void loyalCustomerGetsTenPercentAfterSurcharges() {
            givenRequest(processing(ServiceType.INSTALLATION, ExecutionMode.ON_SITE, Priority.URGENT));
            givenCompletedRequests(3); // boundary: exactly three COMPLETED requests

            OfferResponseDto offer = service.createOffer(ID, "Install boiler");

            // (120 + 30 + 40) * 10% = 19.00; a discount on the base price only would be 12.00
            assertEquals(new BigDecimal("19.00"), offer.priceBreakdown().loyaltyDiscount());
            assertEquals(new BigDecimal("171.00"), offer.finalPrice());
        }

        @Test
        void customerWithTwoCompletedRequestsIsNotLoyal() {
            givenRequest(processing(ServiceType.INSTALLATION, ExecutionMode.ON_SITE, Priority.URGENT));
            givenCompletedRequests(2);

            OfferResponseDto offer = service.createOffer(ID, "Install boiler");

            assertEquals(new BigDecimal("0.00"), offer.priceBreakdown().loyaltyDiscount());
            assertEquals(new BigDecimal("190.00"), offer.finalPrice());
        }

        @Test
        void loyaltyCountsOnlyCompletedRequestsOfTheSameCustomer() {
            givenRequest(processing(ServiceType.REPAIR, ExecutionMode.REMOTE, Priority.STANDARD));
            givenCompletedRequests(0);

            service.createOffer(ID, "Repair");

            verify(repository).countByCustomerInfoCustomerEmailIgnoreCaseAndStatus(EMAIL, RequestStatus.COMPLETED);
        }

        @Test
        void requestThatIsNotProcessingCannotGetAnOffer() {
            ServiceRequest request = submitted(ExecutionMode.REMOTE);
            givenRequest(request);
            givenCompletedRequests(0);

            assertThrows(InvalidStatusTransitionException.class, () -> service.createOffer(ID, "Repair"));

            assertEquals(RequestStatus.SUBMITTED, request.getStatus());
            verify(repository, never()).flush();
        }

        @Test
        void missingRequest() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThrows(ServiceRequestNotFoundException.class, () -> service.createOffer(ID, "Repair"));
        }
    }

    @Nested
    class AcceptOffer {

        @Test
        void offerCanBeAcceptedAtExactlyValidUntil() {
            ServiceRequest request = offered(NOW.minusDays(3));
            givenRequest(request);

            ServiceRequestResponseDto result = service.acceptOffer(ID);

            assertEquals(RequestStatus.ACCEPTED, result.status());
        }

        @Test
        void expiredOfferCannotBeAcceptedAndRequestBecomesExpired() {
            ServiceRequest request = offered(NOW.minusDays(3).minusSeconds(1));
            givenRequest(request);

            assertThrows(OfferExpiredException.class, () -> service.acceptOffer(ID));

            assertEquals(RequestStatus.EXPIRED, request.getStatus());
        }
    }

    @Nested
    class ExpireOverdueOffers {

        @Test
        void overdueOffersAreExpired() {
            ServiceRequest overdue = offered(NOW.minusDays(4));
            when(repository.findByStatusAndOfferValidUntilBefore(RequestStatus.OFFERED, NOW))
                    .thenReturn(List.of(overdue));

            int expired = service.expireOverdueOffers();

            assertEquals(1, expired);
            assertEquals(RequestStatus.EXPIRED, overdue.getStatus());
        }
    }

    @Nested
    class Schedule {

        @Test
        void remoteServiceCanStartExactlyFourHoursLater() {
            givenRequest(accepted(ExecutionMode.REMOTE));

            ServiceRequestResponseDto result = service.schedule(ID, NOW.plusHours(4), NOW.plusHours(6));

            assertEquals(RequestStatus.SCHEDULED, result.status());
            assertEquals(NOW.plusHours(4), result.execution().expectedStart());
            assertEquals(NOW.plusHours(6), result.execution().expectedEnd());
        }

        @Test
        void remoteServiceCannotStartEarlierThanFourHours() {
            ServiceRequest request = accepted(ExecutionMode.REMOTE);
            givenRequest(request);

            assertThrows(InvalidScheduleException.class,
                    () -> service.schedule(ID, NOW.plusHours(4).minusSeconds(1), NOW.plusHours(6)));
            assertEquals(RequestStatus.ACCEPTED, request.getStatus());
        }

        @Test
        void onSiteServiceScheduledOnFridayCanStartOnMonday() {
            givenRequest(accepted(ExecutionMode.ON_SITE));
            LocalDateTime monday = LocalDateTime.of(2026, 11, 2, 0, 0);

            ServiceRequestResponseDto result = service.schedule(ID, monday, monday.plusHours(2));

            assertEquals(RequestStatus.SCHEDULED, result.status());
        }

        @Test
        void onSiteServiceScheduledOnFridayCannotStartOnTheWeekend() {
            givenRequest(accepted(ExecutionMode.ON_SITE));
            LocalDateTime sunday = LocalDateTime.of(2026, 11, 1, 23, 59);

            assertThrows(InvalidScheduleException.class, () -> service.schedule(ID, sunday, sunday.plusHours(2)));
        }

        @Test
        void onlyAcceptedRequestCanBeScheduled() {
            givenRequest(submitted(ExecutionMode.REMOTE));

            assertThrows(InvalidStatusTransitionException.class,
                    () -> service.schedule(ID, NOW.plusHours(5), NOW.plusHours(6)));
        }
    }

    @Nested
    class Cancel {

        @Test
        void onSiteServiceCanBeCancelledExactly24HoursBeforeStart() {
            givenRequest(scheduled(ExecutionMode.ON_SITE, NOW.plusHours(24)));

            ServiceRequestResponseDto result = service.cancel(ID, "Changed plans");

            assertEquals(RequestStatus.CANCELLED, result.status());
            assertEquals("Changed plans", result.cancellationReason());
        }

        @Test
        void onSiteServiceCannotBeCancelledLessThan24HoursBeforeStart() {
            ServiceRequest request = scheduled(ExecutionMode.ON_SITE, NOW.plusHours(24).minusSeconds(1));
            givenRequest(request);

            assertThrows(CancellationDeadlinePassedException.class, () -> service.cancel(ID, "Changed plans"));
            assertEquals(RequestStatus.SCHEDULED, request.getStatus());
        }

        @Test
        void remoteServiceCanBeCancelledUntil4HoursBeforeStart() {
            givenRequest(scheduled(ExecutionMode.REMOTE, NOW.plusHours(4)));

            assertEquals(RequestStatus.CANCELLED, service.cancel(ID, "Changed plans").status());
        }

        @Test
        void remoteServiceCannotBeCancelledLessThan4HoursBeforeStart() {
            givenRequest(scheduled(ExecutionMode.REMOTE, NOW.plusHours(4).minusSeconds(1)));

            assertThrows(CancellationDeadlinePassedException.class, () -> service.cancel(ID, "Changed plans"));
        }

        @Test
        void requestInProgressCannotBeCancelled() {
            ServiceRequest request = scheduled(ExecutionMode.REMOTE, NOW.plusDays(2));
            request.startExecution(NOW);
            givenRequest(request);

            assertThrows(InvalidStatusTransitionException.class, () -> service.cancel(ID, "Changed plans"));
        }
    }

    // --- test data: requests are moved through their real lifecycle, so they are in a consistent state ---

    private void givenRequest(ServiceRequest request) {
        when(repository.findById(ID)).thenReturn(Optional.of(request));
    }

    private void givenCompletedRequests(long count) {
        when(repository.countByCustomerInfoCustomerEmailIgnoreCaseAndStatus(anyString(), any())).thenReturn(count);
    }

    private static ServiceRequest submitted(ExecutionMode mode) {
        return submitted(ServiceType.REPAIR, mode, Priority.STANDARD, NOW.minusDays(5));
    }

    private static ServiceRequest submitted(ServiceType type, ExecutionMode mode, Priority priority,
                                            LocalDateTime submittedAt) {
        return new ServiceRequest(new CustomerInfo("Ivan", EMAIL), "Some work to be done", type, mode, priority,
                "Varna", "Flooding", submittedAt);
    }

    private static ServiceRequest processing(ServiceType type, ExecutionMode mode, Priority priority) {
        ServiceRequest request = submitted(type, mode, priority, NOW.minusDays(1));
        request.startProcessing();
        return request;
    }

    private static ServiceRequest offered(LocalDateTime offerCreatedAt) {
        ServiceRequest request = submitted(ServiceType.REPAIR, ExecutionMode.REMOTE, Priority.STANDARD,
                offerCreatedAt.minusDays(1));
        request.startProcessing();
        request.createOffer("Repair", false, offerCreatedAt);
        return request;
    }

    private static ServiceRequest accepted(ExecutionMode mode) {
        ServiceRequest request = submitted(ServiceType.REPAIR, mode, Priority.STANDARD, NOW.minusDays(2));
        request.startProcessing();
        request.createOffer("Repair", false, NOW.minusDays(1));
        request.acceptOffer(NOW.minusDays(1));
        return request;
    }

    private static ServiceRequest scheduled(ExecutionMode mode, LocalDateTime expectedStart) {
        ServiceRequest request = accepted(mode);
        // scheduled a week earlier, so the earliest-start rule does not depend on the test's NOW
        request.schedule(expectedStart, expectedStart.plusHours(2), NOW.minusDays(7));
        return request;
    }
}
