package com.digitalbank.accountopening.common.exception;

import com.digitalbank.accountopening.common.response.ErrorResponse;
import com.digitalbank.accountopening.integration.cifkyc.CifKycClientException;
import com.digitalbank.accountopening.integration.cifkyc.CifKycVerificationErrorCode;
import com.digitalbank.accountopening.integration.cifkyc.CifKycVerificationException;
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
