package com.digitalbank.accountopening.common.exception;

public class ApplicationNotEditableException extends RuntimeException {

    public ApplicationNotEditableException() {
        super("Only draft applications can be updated");
    }
}
