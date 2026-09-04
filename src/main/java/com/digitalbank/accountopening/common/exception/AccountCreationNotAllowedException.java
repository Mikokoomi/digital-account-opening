package com.digitalbank.accountopening.common.exception;

import com.digitalbank.accountopening.application.enums.ApplicationStatus;
public class AccountCreationNotAllowedException extends RuntimeException {
    public AccountCreationNotAllowedException(ApplicationStatus status) { super("Account creation is not allowed for application status: " + status); }
}
