package com.digitalbank.accountopening.common.exception;

import com.digitalbank.accountopening.application.enums.ApplicationStatus;

public class InvalidApplicationStatusTransitionException extends RuntimeException {

    public InvalidApplicationStatusTransitionException(
            ApplicationStatus currentStatus,
            ApplicationStatus targetStatus
    ) {
        super("Application status transition from "
                + currentStatus + " to " + targetStatus + " is not allowed.");
    }
}
