package com.digitalbank.accountopening.common.exception;

public class ApplicationMandatoryConditionsNotSatisfiedException extends RuntimeException {

    public ApplicationMandatoryConditionsNotSatisfiedException() {
        super("Application does not satisfy the mandatory business conditions");
    }
}
