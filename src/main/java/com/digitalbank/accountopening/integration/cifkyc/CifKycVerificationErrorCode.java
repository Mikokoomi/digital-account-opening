package com.digitalbank.accountopening.integration.cifkyc;

public enum CifKycVerificationErrorCode {
    CUSTOMER_NOT_FOUND("Customer was not found"),
    CUSTOMER_NOT_ACTIVE("Customer is not active"),
    KYC_NOT_VERIFIED("Customer KYC is not verified"),
    KYC_EXPIRED("Customer KYC has expired");

    private final String message;

    CifKycVerificationErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
