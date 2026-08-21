package com.digitalbank.accountopening.application.dto;

import com.digitalbank.accountopening.application.enums.ApplicationStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ApplicationResponse(
        UUID applicationId,
        String customerId,
        String productCode,
        String productName,
        ApplicationStatus status,
        String kycStatus,
        OffsetDateTime cifVerifiedAt,
        LocalDate kycExpiryDate,
        String rejectReason,
        OffsetDateTime submittedAt,
        OffsetDateTime cancelledAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
