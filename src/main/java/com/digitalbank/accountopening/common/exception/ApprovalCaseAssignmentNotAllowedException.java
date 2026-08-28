package com.digitalbank.accountopening.common.exception;

import com.digitalbank.accountopening.approval.ApprovalCaseStatus;

public class ApprovalCaseAssignmentNotAllowedException extends RuntimeException {

    public ApprovalCaseAssignmentNotAllowedException(ApprovalCaseStatus status) {
        super("Approval case assignment is not allowed for status: " + status);
    }
}
