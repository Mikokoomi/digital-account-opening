package com.digitalbank.accountopening.integration.corebanking;

public class CoreBankingDefinitiveFailureException extends CoreBankingNonRetryableException {
    public CoreBankingDefinitiveFailureException(String message, Integer httpStatus, Throwable cause) {
        super(message, httpStatus, cause);
    }
}
