package com.digitalbank.accountopening.approval.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectApprovalCaseRequest(
        @NotBlank String staffId,
        @NotBlank String reason
) {
}
