package com.digitalbank.accountopening.common.exception;

public class ManualReviewDataInvalidException extends RuntimeException {

    public ManualReviewDataInvalidException() {
        super("Application manual-review snapshot is invalid");
    }
}
