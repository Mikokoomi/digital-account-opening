package com.digitalbank.accountopening.integration.cifkyc;

import java.time.LocalDate;

public record CifKycVerificationResult(
        String customerId,
        String fullName,
        LocalDate dateOfBirth,
        boolean eligible,
        String customerStatus,
        String kycStatus,
        LocalDate kycExpiryDate,
        boolean reviewRequired,
        ReviewReason reviewReason
) {
}
