package com.digitalbank.accountopening.common.exception;

public class ApplicationKycVerificationRequiredException extends RuntimeException {

    public ApplicationKycVerificationRequiredException() {
        super("Current KYC verification is required before processing application");
    }
}
