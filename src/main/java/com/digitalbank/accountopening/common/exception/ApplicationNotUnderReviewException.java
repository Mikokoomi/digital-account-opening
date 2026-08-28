package com.digitalbank.accountopening.common.exception;

import com.digitalbank.accountopening.application.enums.ApplicationStatus;

public class ApplicationNotUnderReviewException extends RuntimeException {

    public ApplicationNotUnderReviewException(ApplicationStatus status) {
        super("Application is not under review. Current status: " + status);
    }
}
