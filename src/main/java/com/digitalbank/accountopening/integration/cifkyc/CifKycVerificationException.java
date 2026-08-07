package com.digitalbank.accountopening.integration.cifkyc;

public class CifKycVerificationException extends RuntimeException {

    private final CifKycVerificationErrorCode errorCode;

    public CifKycVerificationException(CifKycVerificationErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CifKycVerificationErrorCode getErrorCode() {
        return errorCode;
    }
}
