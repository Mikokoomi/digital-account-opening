package com.digitalbank.accountopening.common.exception;

public class AccountCreationInProgressException extends RuntimeException {
    public AccountCreationInProgressException() { super("Account creation is already in progress"); }
}
