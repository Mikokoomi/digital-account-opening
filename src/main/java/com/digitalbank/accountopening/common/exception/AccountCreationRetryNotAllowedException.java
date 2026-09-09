package com.digitalbank.accountopening.common.exception;

import com.digitalbank.accountopening.application.enums.ApplicationStatus;
public class AccountCreationRetryNotAllowedException extends RuntimeException {
    public AccountCreationRetryNotAllowedException(ApplicationStatus status) { super("Account creation retry is not allowed for application status: " + status); }
}
