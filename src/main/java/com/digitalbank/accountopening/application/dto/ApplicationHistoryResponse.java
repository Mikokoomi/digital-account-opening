package com.digitalbank.accountopening.application.dto;

import com.digitalbank.accountopening.application.enums.ApplicationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApplicationHistoryResponse(
        UUID historyId,
        ApplicationStatus fromStatus,
        ApplicationStatus toStatus,
        String changedBy,
        String reason,
        OffsetDateTime changedAt
) {
}
