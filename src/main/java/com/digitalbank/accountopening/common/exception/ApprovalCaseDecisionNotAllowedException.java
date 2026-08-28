package com.digitalbank.accountopening.common.exception;

import com.digitalbank.accountopening.approval.ApprovalCaseStatus;

public class ApprovalCaseDecisionNotAllowedException extends RuntimeException {

    public ApprovalCaseDecisionNotAllowedException(ApprovalCaseStatus status) {
        super("Approval case decision is not allowed for status: " + status);
    }
}
