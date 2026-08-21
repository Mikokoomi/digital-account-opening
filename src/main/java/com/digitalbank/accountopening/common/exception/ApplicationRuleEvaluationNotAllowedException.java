package com.digitalbank.accountopening.common.exception;

import com.digitalbank.accountopening.application.enums.ApplicationStatus;

public class ApplicationRuleEvaluationNotAllowedException extends RuntimeException {

    public ApplicationRuleEvaluationNotAllowedException(ApplicationStatus status) {
        super("Business rule evaluation is not allowed for application status: " + status);
    }
}
