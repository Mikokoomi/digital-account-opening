package com.digitalbank.accountopening.common.exception;

public class KycAlreadyVerifiedException extends RuntimeException {

    public KycAlreadyVerifiedException() {
        super("Application already has a successful KYC verification");
    }
}