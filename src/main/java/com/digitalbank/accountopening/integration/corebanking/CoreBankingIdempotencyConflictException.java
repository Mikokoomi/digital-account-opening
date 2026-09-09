package com.digitalbank.accountopening.integration.corebanking;

public class CoreBankingIdempotencyConflictException extends CoreBankingNonRetryableException {
    public CoreBankingIdempotencyConflictException(Throwable cause) { super("Core Banking rejected the idempotency key", 409, cause); }
}
