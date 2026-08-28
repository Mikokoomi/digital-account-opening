package com.digitalbank.accountopening.common.exception;

import java.util.UUID;

public class ApprovalCaseNotFoundException extends RuntimeException {

    public ApprovalCaseNotFoundException(UUID caseId) {
        super("Approval case not found: " + caseId);
    }
}
