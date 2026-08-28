package com.digitalbank.accountopening.common.exception;

import java.util.UUID;

public class ApprovalCaseAlreadyExistsException extends RuntimeException {

    public ApprovalCaseAlreadyExistsException(UUID applicationId) {
        super("Approval case already exists for application: " + applicationId);
    }
}
