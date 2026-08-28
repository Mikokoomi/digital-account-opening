package com.digitalbank.accountopening.common.exception;

public class ApprovalCaseBusinessRulesNotSatisfiedException extends RuntimeException {

    public ApprovalCaseBusinessRulesNotSatisfiedException() {
        super("Application no longer satisfies the required business rules");
    }
}
