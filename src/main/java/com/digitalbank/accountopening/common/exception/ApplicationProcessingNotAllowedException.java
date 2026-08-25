package com.digitalbank.accountopening.common.exception;

import com.digitalbank.accountopening.application.enums.ApplicationStatus;

public class ApplicationProcessingNotAllowedException extends RuntimeException {

    public ApplicationProcessingNotAllowedException(ApplicationStatus status) {
        super("Application processing is not allowed for application status: " + status);
    }
}
