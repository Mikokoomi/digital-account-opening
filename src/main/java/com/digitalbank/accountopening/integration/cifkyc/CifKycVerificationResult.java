package com.digitalbank.accountopening.integration.cifkyc;

import java.time.LocalDate;

public record CifKycVerificationResult(
        String customerId,
        boolean eligible,
        String customerStatus,
        String kycStatus,
        LocalDate kycExpiryDate
) {
}
