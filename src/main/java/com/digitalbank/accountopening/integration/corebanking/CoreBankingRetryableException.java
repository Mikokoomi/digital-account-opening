package com.digitalbank.accountopening.integration.corebanking;

public class CoreBankingRetryableException extends RuntimeException {
    private final Integer httpStatus;
    public CoreBankingRetryableException(String message, Integer httpStatus, Throwable cause) { super(message, cause); this.httpStatus=httpStatus; }
    public Integer getHttpStatus() { return httpStatus; }
}
