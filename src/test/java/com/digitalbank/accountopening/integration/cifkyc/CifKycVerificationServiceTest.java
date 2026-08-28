package com.digitalbank.accountopening.integration.cifkyc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CifKycVerificationServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 7);

    @Mock
    private CifKycClient cifKycClient;

    private CifKycVerificationService verificationService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-08-07T00:00:00Z"),
                ZoneOffset.UTC
        );
        verificationService = new CifKycVerificationService(cifKycClient, fixedClock);
    }

    @Test
    void verify_shouldReturnEligibleResultForValidCustomer() {
        CifKycCustomerResponse customer = customer("CUS001", "ACTIVE", "VERIFIED", TODAY.plusDays(1));
        when(cifKycClient.getCustomer("CUS001")).thenReturn(Optional.of(customer));

        CifKycVerificationResult result = verificationService.verify("CUS001");

        assertEquals("CUS001", result.customerId());
        assertTrue(result.eligible());
        assertEquals("ACTIVE", result.customerStatus());
        assertEquals("VERIFIED", result.kycStatus());
        assertEquals(TODAY.plusDays(1), result.kycExpiryDate());
        assertEquals(false, result.reviewRequired());
        assertEquals(null, result.reviewReason());
    }

    @Test
    void verify_shouldRejectMissingCustomer() {
        when(cifKycClient.getCustomer("CUS999")).thenReturn(Optional.empty());

        assertVerificationFailure(
                "CUS999",
                CifKycVerificationErrorCode.CUSTOMER_NOT_FOUND
        );
    }

    @Test
    void verify_shouldRejectBlockedCustomer() {
        when(cifKycClient.getCustomer("CUS004")).thenReturn(Optional.of(
                customer("CUS004", "BLOCKED", "VERIFIED", TODAY.plusDays(1))
        ));

        assertVerificationFailure(
                "CUS004",
                CifKycVerificationErrorCode.CUSTOMER_NOT_ACTIVE
        );
    }

    @Test
    void verify_shouldRejectInactiveCustomer() {
        when(cifKycClient.getCustomer("CUS005")).thenReturn(Optional.of(
                customer("CUS005", "INACTIVE", "VERIFIED", TODAY.plusDays(1))
        ));

        assertVerificationFailure(
                "CUS005",
                CifKycVerificationErrorCode.CUSTOMER_NOT_ACTIVE
        );
    }

    @Test
    void verify_shouldRejectPendingKyc() {
        when(cifKycClient.getCustomer("CUS003")).thenReturn(Optional.of(
                customer("CUS003", "ACTIVE", "PENDING", TODAY.plusDays(1))
        ));

        assertVerificationFailure(
                "CUS003",
                CifKycVerificationErrorCode.KYC_NOT_VERIFIED
        );
    }

    @Test
    void verify_shouldRejectRejectedKyc() {
        when(cifKycClient.getCustomer("CUS006")).thenReturn(Optional.of(
                customer("CUS006", "ACTIVE", "REJECTED", TODAY.plusDays(1))
        ));

        assertVerificationFailure(
                "CUS006",
                CifKycVerificationErrorCode.KYC_NOT_VERIFIED
        );
    }

    @Test
    void verify_shouldRejectExpiredKyc() {
        when(cifKycClient.getCustomer("CUS002")).thenReturn(Optional.of(
                customer("CUS002", "ACTIVE", "VERIFIED", TODAY.minusDays(1))
        ));

        assertVerificationFailure(
                "CUS002",
                CifKycVerificationErrorCode.KYC_EXPIRED
        );
    }

    @Test
    void verify_shouldAcceptKycThatExpiresToday() {
        when(cifKycClient.getCustomer("CUS001")).thenReturn(Optional.of(
                customer("CUS001", "ACTIVE", "VERIFIED", TODAY)
        ));

        CifKycVerificationResult result = verificationService.verify("CUS001");

        assertTrue(result.eligible());
        assertEquals(TODAY, result.kycExpiryDate());
    }

    @Test
    void verify_shouldCheckCustomerStatusBeforeKycStatusAndExpiry() {
        when(cifKycClient.getCustomer("CUS004")).thenReturn(Optional.of(
                customer("CUS004", "BLOCKED", "PENDING", TODAY.minusDays(1))
        ));

        assertVerificationFailure(
                "CUS004",
                CifKycVerificationErrorCode.CUSTOMER_NOT_ACTIVE
        );
    }

    @Test
    void verify_shouldPropagateIntegrationFailure() {
        CifKycClientException clientException = new CifKycClientException(
                "CIF/KYC service is unavailable"
        );
        when(cifKycClient.getCustomer("CUS001")).thenThrow(clientException);

        CifKycClientException thrownException = assertThrows(
                CifKycClientException.class,
                () -> verificationService.verify("CUS001")
        );

        assertSame(clientException, thrownException);
    }

    @Test
    void verify_shouldTreatMissingCustomerStatusAsIntegrationFailure() {
        assertIncompleteResponseFailure(
                customer("CUS001", null, "VERIFIED", TODAY.plusDays(1))
        );
    }

    @Test
    void verify_shouldTreatMissingKycStatusAsIntegrationFailure() {
        assertIncompleteResponseFailure(
                customer("CUS001", "ACTIVE", null, TODAY.plusDays(1))
        );
    }

    @Test
    void verify_shouldTreatMissingKycExpiryDateAsIntegrationFailure() {
        assertIncompleteResponseFailure(
                customer("CUS001", "ACTIVE", "VERIFIED", null)
        );
    }

    @Test
    void verify_shouldReturnValidManualReviewSignal() {
        CifKycCustomerResponse customer = new CifKycCustomerResponse(
                "CUS002", "Tran Thi B", LocalDate.of(2000, 8, 20),
                "ACTIVE", "VERIFIED", TODAY.plusDays(1),
                true, ReviewReason.CUSTOMER_PROFILE_REVIEW
        );
        when(cifKycClient.getCustomer("CUS002")).thenReturn(Optional.of(customer));

        CifKycVerificationResult result = verificationService.verify("CUS002");

        assertTrue(result.reviewRequired());
        assertEquals(ReviewReason.CUSTOMER_PROFILE_REVIEW, result.reviewReason());
    }

    @Test
    void verify_shouldTreatMissingManualReviewReasonAsIntegrationFailure() {
        assertIncompleteResponseFailure(new CifKycCustomerResponse(
                "CUS001", "Nguyen Van A", LocalDate.of(1998, 5, 15),
                "ACTIVE", "VERIFIED", TODAY.plusDays(1), true, null
        ));
    }

    @Test
    void verify_shouldTreatUnexpectedManualReviewReasonAsIntegrationFailure() {
        assertIncompleteResponseFailure(new CifKycCustomerResponse(
                "CUS001", "Nguyen Van A", LocalDate.of(1998, 5, 15),
                "ACTIVE", "VERIFIED", TODAY.plusDays(1),
                false, ReviewReason.SPECIAL_HANDLING_REQUIRED
        ));
    }

    private void assertIncompleteResponseFailure(CifKycCustomerResponse customer) {
        when(cifKycClient.getCustomer("CUS001")).thenReturn(Optional.of(customer));

        CifKycClientException exception = assertThrows(
                CifKycClientException.class,
                () -> verificationService.verify("CUS001")
        );

        assertEquals(
                "CIF/KYC service returned an incomplete customer response",
                exception.getMessage()
        );
    }

    private void assertVerificationFailure(
            String customerId,
            CifKycVerificationErrorCode expectedErrorCode
    ) {
        CifKycVerificationException exception = assertThrows(
                CifKycVerificationException.class,
                () -> verificationService.verify(customerId)
        );

        assertEquals(expectedErrorCode, exception.getErrorCode());
        assertEquals(expectedErrorCode.getMessage(), exception.getMessage());
    }

    private CifKycCustomerResponse customer(
            String customerId,
            String customerStatus,
            String kycStatus,
            LocalDate kycExpiryDate
    ) {
        return new CifKycCustomerResponse(
                customerId,
                "Nguyen Van A",
                LocalDate.of(1998, 5, 15),
                customerStatus,
                kycStatus,
                kycExpiryDate,
                false,
                null
        );
    }
}
