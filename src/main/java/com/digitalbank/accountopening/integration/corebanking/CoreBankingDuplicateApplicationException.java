package com.digitalbank.accountopening.integration.corebanking;

public class CoreBankingDuplicateApplicationException extends RuntimeException {
    public CoreBankingDuplicateApplicationException() { super("Core Banking already has an account for this application"); }
}
