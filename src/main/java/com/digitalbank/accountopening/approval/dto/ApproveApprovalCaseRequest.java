package com.digitalbank.accountopening.approval.dto;

import jakarta.validation.constraints.NotBlank;

public record ApproveApprovalCaseRequest(
        @NotBlank String staffId
) {
}
