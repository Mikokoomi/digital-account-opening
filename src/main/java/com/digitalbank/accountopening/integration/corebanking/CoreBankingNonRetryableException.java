package com.digitalbank.accountopening.integration.corebanking;

public class CoreBankingNonRetryableException extends RuntimeException {
    private final Integer httpStatus;
    public CoreBankingNonRetryableException(String message, Integer httpStatus) { super(message); this.httpStatus=httpStatus; }
    public CoreBankingNonRetryableException(String message, Integer httpStatus, Throwable cause) { super(message,cause); this.httpStatus=httpStatus; }
    public Integer getHttpStatus() { return httpStatus; }
}
