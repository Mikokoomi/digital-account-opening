package com.digitalbank.accountopening.common.exception;

public class ApplicationNotSubmittableException extends RuntimeException {

    public ApplicationNotSubmittableException() {
        super("Only draft applications can be submitted");
    }
}
