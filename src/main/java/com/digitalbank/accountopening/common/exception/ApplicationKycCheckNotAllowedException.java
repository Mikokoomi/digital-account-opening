package com.digitalbank.accountopening.common.exception;

import com.digitalbank.accountopening.application.enums.ApplicationStatus;

public class ApplicationKycCheckNotAllowedException extends RuntimeException {

    public ApplicationKycCheckNotAllowedException(ApplicationStatus status) {
        super("KYC check is not allowed for application status: " + status);
    }
}
