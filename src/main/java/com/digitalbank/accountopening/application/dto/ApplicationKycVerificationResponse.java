package com.digitalbank.accountopening.application.dto;

import java.time.LocalDate;
import java.util.UUID;
import com.digitalbank.accountopening.integration.cifkyc.ReviewReason;

public record ApplicationKycVerificationResponse(
        UUID applicationId,
        String customerId,
        boolean eligible,
        String customerStatus,
        String kycStatus,
        LocalDate kycExpiryDate,
        boolean reviewRequired,
        ReviewReason reviewReason
) {
}
