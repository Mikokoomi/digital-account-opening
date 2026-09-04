package com.digitalbank.accountopening.common.exception;

import com.digitalbank.accountopening.common.response.ErrorResponse;
import com.digitalbank.accountopening.integration.cifkyc.CifKycClientException;
import com.digitalbank.accountopening.integration.cifkyc.CifKycVerificationErrorCode;
import com.digitalbank.accountopening.integration.cifkyc.CifKycVerificationException;
import com.digitalbank.accountopening.integration.corebanking.CoreBankingClientException;
import com.digitalbank.accountopening.integration.corebanking.CoreBankingDuplicateApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountCreationNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleAccountCreationNotAllowed(AccountCreationNotAllowedException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), "ACCOUNT_CREATION_NOT_ALLOWED");
    }

    @ExceptionHandler({BankAccountAlreadyExistsException.class, CoreBankingDuplicateApplicationException.class})
    public ResponseEntity<ErrorResponse> handleBankAccountAlreadyExists(RuntimeException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), "BANK_ACCOUNT_ALREADY_EXISTS");
    }

    @ExceptionHandler(CoreBankingClientException.class)
    public ResponseEntity<ErrorResponse> handleCoreBankingClient(CoreBankingClientException exception) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "Core Banking service is unavailable", "CORE_BANKING_SERVICE_UNAVAILABLE");
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(
            ProductNotFoundException exception
    ) {
        ErrorResponse response = new ErrorResponse(
                false,
                exception.getMessage(),
                "PRODUCT_NOT_FOUND",
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(ProductInactiveException.class)
    public ResponseEntity<ErrorResponse> handleProductInactive(
            ProductInactiveException exception
    ) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), "PRODUCT_INACTIVE");
    }

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleApplicationNotFound(
            ApplicationNotFoundException exception
    ) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), "APPLICATION_NOT_FOUND");
    }

    @ExceptionHandler(ApplicationNotEditableException.class)
    public ResponseEntity<ErrorResponse> handleApplicationNotEditable(
            ApplicationNotEditableException exception
    ) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), "APPLICATION_NOT_EDITABLE");
    }

    @ExceptionHandler(ApplicationNotSubmittableException.class)
    public ResponseEntity<ErrorResponse> handleApplicationNotSubmittable(
            ApplicationNotSubmittableException exception
    ) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), "APPLICATION_NOT_SUBMITTABLE");
    }

    @ExceptionHandler(ApplicationNotCancellableException.class)
    public ResponseEntity<ErrorResponse> handleApplicationNotCancellable(
            ApplicationNotCancellableException exception
    ) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), "APPLICATION_NOT_CANCELLABLE");
    }

    @ExceptionHandler(ApplicationRuleEvaluationNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleApplicationRuleEvaluationNotAllowed(
            ApplicationRuleEvaluationNotAllowedException exception
    ) {
        return error(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                "APPLICATION_RULE_EVALUATION_NOT_ALLOWED"
        );
    }

    @ExceptionHandler(InvalidApplicationStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidApplicationStatusTransition(
            InvalidApplicationStatusTransitionException exception
    ) {
        return error(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                "INVALID_APPLICATION_STATUS_TRANSITION"
        );
    }

    @ExceptionHandler(ApplicationProcessingNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleApplicationProcessingNotAllowed(
            ApplicationProcessingNotAllowedException exception
    ) {
        return error(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                "APPLICATION_PROCESSING_NOT_ALLOWED"
        );
    }

    @ExceptionHandler(ApplicationKycVerificationRequiredException.class)
    public ResponseEntity<ErrorResponse> handleApplicationKycVerificationRequired(
            ApplicationKycVerificationRequiredException exception
    ) {
        return error(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                "APPLICATION_KYC_VERIFICATION_REQUIRED"
        );
    }

    @ExceptionHandler(ApplicationMandatoryConditionsNotSatisfiedException.class)
    public ResponseEntity<ErrorResponse> handleApplicationMandatoryConditionsNotSatisfied(
            ApplicationMandatoryConditionsNotSatisfiedException exception
    ) {
        return error(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                "APPLICATION_MANDATORY_CONDITIONS_NOT_SATISFIED"
        );
    }

    @ExceptionHandler(ManualReviewDataInvalidException.class)
    public ResponseEntity<ErrorResponse> handleManualReviewDataInvalid(
            ManualReviewDataInvalidException exception
    ) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), "MANUAL_REVIEW_DATA_INVALID");
    }

    @ExceptionHandler(ApprovalCaseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleApprovalCaseNotFound(
            ApprovalCaseNotFoundException exception
    ) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), "APPROVAL_CASE_NOT_FOUND");
    }

    @ExceptionHandler(ApprovalCaseAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleApprovalCaseAlreadyExists(
            ApprovalCaseAlreadyExistsException exception
    ) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), "APPROVAL_CASE_ALREADY_EXISTS");
    }

    @ExceptionHandler(ApprovalCaseAssignmentNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleApprovalCaseAssignmentNotAllowed(
            ApprovalCaseAssignmentNotAllowedException exception
    ) {
        return error(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                "APPROVAL_CASE_ASSIGNMENT_NOT_ALLOWED"
        );
    }

    @ExceptionHandler(ApprovalCaseDecisionNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleApprovalCaseDecisionNotAllowed(
            ApprovalCaseDecisionNotAllowedException exception
    ) {
        return error(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                "APPROVAL_CASE_DECISION_NOT_ALLOWED"
        );
    }

    @ExceptionHandler(ApprovalCaseStaffMismatchException.class)
    public ResponseEntity<ErrorResponse> handleApprovalCaseStaffMismatch(
            ApprovalCaseStaffMismatchException exception
    ) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), "APPROVAL_CASE_STAFF_MISMATCH");
    }

    @ExceptionHandler(ApprovalCaseBusinessRulesNotSatisfiedException.class)
    public ResponseEntity<ErrorResponse> handleApprovalCaseBusinessRulesNotSatisfied(
            ApprovalCaseBusinessRulesNotSatisfiedException exception
    ) {
        return error(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                "APPROVAL_CASE_BUSINESS_RULES_NOT_SATISFIED"
        );
    }

    @ExceptionHandler(ApplicationNotUnderReviewException.class)
    public ResponseEntity<ErrorResponse> handleApplicationNotUnderReview(
            ApplicationNotUnderReviewException exception
    ) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), "APPLICATION_NOT_UNDER_REVIEW");
    }

    @ExceptionHandler(ApplicationKycCheckNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleApplicationKycCheckNotAllowed(
            ApplicationKycCheckNotAllowedException exception
    ) {
        return error(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                "APPLICATION_KYC_CHECK_NOT_ALLOWED"
        );
    }

    @ExceptionHandler(KycAlreadyVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleKycAlreadyVerified(
                KycAlreadyVerifiedException exception
        ) {
        return error(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                "KYC_ALREADY_VERIFIED"
        );
        }
    @ExceptionHandler(CifKycVerificationException.class)
    public ResponseEntity<ErrorResponse> handleCifKycVerification(
            CifKycVerificationException exception
    ) {
        CifKycVerificationErrorCode errorCode = exception.getErrorCode();
        HttpStatus status = errorCode == CifKycVerificationErrorCode.CUSTOMER_NOT_FOUND
                ? HttpStatus.NOT_FOUND
                : HttpStatus.CONFLICT;

        return error(status, exception.getMessage(), errorCode.name());
    }

    @ExceptionHandler(CifKycClientException.class)
    public ResponseEntity<ErrorResponse> handleCifKycClient() {
        return error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "CIF/KYC service is unavailable",
                "CIF_KYC_SERVICE_UNAVAILABLE"
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        FieldError fieldError = exception.getBindingResult().getFieldErrors().getFirst();
        String message = "Validation failed for field '%s': %s".formatted(
                fieldError.getField(),
                fieldError.getDefaultMessage()
        );
        return error(HttpStatus.BAD_REQUEST, message, "VALIDATION_ERROR");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        return error(HttpStatus.BAD_REQUEST, "Invalid application ID.", "INVALID_UUID");
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status,
            String message,
            String errorCode
    ) {
        ErrorResponse response = new ErrorResponse(false, message, errorCode, LocalDateTime.now());
        return ResponseEntity.status(status).body(response);
    }
}
