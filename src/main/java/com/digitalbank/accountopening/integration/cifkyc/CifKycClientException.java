package com.digitalbank.accountopening.integration.cifkyc;

public class CifKycClientException extends RuntimeException {

    public CifKycClientException(String message) {
        super(message);
    }

    public CifKycClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
