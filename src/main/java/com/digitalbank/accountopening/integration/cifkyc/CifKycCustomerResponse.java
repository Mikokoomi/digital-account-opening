package com.digitalbank.accountopening.integration.cifkyc;

import java.time.LocalDate;

public record CifKycCustomerResponse(
        String customerId,
        String fullName,
        LocalDate dateOfBirth,
        String customerStatus,
        String kycStatus,
        LocalDate kycExpiryDate,
        Boolean reviewRequired,
        ReviewReason reviewReason
) {
}
