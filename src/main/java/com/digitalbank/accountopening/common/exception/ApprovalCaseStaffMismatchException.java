package com.digitalbank.accountopening.common.exception;

public class ApprovalCaseStaffMismatchException extends RuntimeException {

    public ApprovalCaseStaffMismatchException() {
        super("Approval case is assigned to another staff member");
    }
}
