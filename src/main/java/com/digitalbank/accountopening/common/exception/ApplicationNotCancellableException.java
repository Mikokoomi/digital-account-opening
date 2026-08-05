package com.digitalbank.accountopening.common.exception;

public class ApplicationNotCancellableException extends RuntimeException {

    public ApplicationNotCancellableException() {
        super("Application cannot be cancelled in its current status");
    }
}
