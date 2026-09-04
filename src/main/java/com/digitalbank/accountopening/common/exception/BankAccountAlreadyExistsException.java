package com.digitalbank.accountopening.common.exception;

public class BankAccountAlreadyExistsException extends RuntimeException {
    public BankAccountAlreadyExistsException() { super("A bank account already exists for this application"); }
}
