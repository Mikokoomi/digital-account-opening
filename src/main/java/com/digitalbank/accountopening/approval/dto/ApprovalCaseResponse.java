package com.digitalbank.accountopening.approval.dto;

import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import com.digitalbank.accountopening.approval.ApprovalCaseStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApprovalCaseResponse(
        UUID caseId,
        UUID applicationId,
        ApplicationStatus applicationStatus,
        String customerId,
        String productCode,
        ApprovalCaseStatus status,
        String assignedTo,
        String reviewReason,
        String decisionReason,
        OffsetDateTime createdAt,
        OffsetDateTime assignedAt,
        OffsetDateTime decidedAt,
        OffsetDateTime updatedAt
) {
}
