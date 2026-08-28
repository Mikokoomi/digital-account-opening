package com.digitalbank.accountopening.integration.cifkyc;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class CifKycVerificationService {

    private static final String ACTIVE = "ACTIVE";
    private static final String VERIFIED = "VERIFIED";

    private final CifKycClient cifKycClient;
    private final Clock clock;

    public CifKycVerificationService(CifKycClient cifKycClient, Clock clock) {
        this.cifKycClient = cifKycClient;
        this.clock = clock;
    }

    public CifKycVerificationResult verify(String customerId) {
        CifKycCustomerResponse customer = cifKycClient.getCustomer(customerId)
                .orElseThrow(() -> new CifKycVerificationException(
                        CifKycVerificationErrorCode.CUSTOMER_NOT_FOUND
                ));

        validateVerificationData(customer);

        if (!ACTIVE.equals(customer.customerStatus())) {
            throw new CifKycVerificationException(
                    CifKycVerificationErrorCode.CUSTOMER_NOT_ACTIVE
            );
        }

        if (!VERIFIED.equals(customer.kycStatus())) {
            throw new CifKycVerificationException(
                    CifKycVerificationErrorCode.KYC_NOT_VERIFIED
            );
        }

        LocalDate today = LocalDate.now(clock);
        if (customer.kycExpiryDate().isBefore(today)) {
            throw new CifKycVerificationException(CifKycVerificationErrorCode.KYC_EXPIRED);
        }

        return new CifKycVerificationResult(
                customer.customerId(),
                true,
                customer.customerStatus(),
                customer.kycStatus(),
                customer.kycExpiryDate(),
                customer.reviewRequired(),
                customer.reviewReason()
        );
    }

    private void validateVerificationData(CifKycCustomerResponse customer) {
        if (customer.customerStatus() == null
                || customer.kycStatus() == null
                || customer.kycExpiryDate() == null
                || customer.reviewRequired() == null
                || customer.reviewRequired() && customer.reviewReason() == null
                || !customer.reviewRequired() && customer.reviewReason() != null) {
            throw new CifKycClientException(
                    "CIF/KYC service returned an incomplete customer response"
            );
        }
    }
}
