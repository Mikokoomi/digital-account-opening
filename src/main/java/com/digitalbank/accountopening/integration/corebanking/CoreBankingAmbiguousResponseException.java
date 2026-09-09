package com.digitalbank.accountopening.integration.corebanking;

public class CoreBankingAmbiguousResponseException extends CoreBankingNonRetryableException {
    public CoreBankingAmbiguousResponseException(String message) {
        super(message, null);
    }
}
