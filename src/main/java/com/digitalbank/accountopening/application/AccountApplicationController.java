package com.digitalbank.accountopening.application;

import com.digitalbank.accountopening.application.dto.ApplicationResponse;
import com.digitalbank.accountopening.application.dto.ApplicationHistoryResponse;
import com.digitalbank.accountopening.application.dto.ApplicationKycVerificationResponse;
import com.digitalbank.accountopening.application.dto.CreateApplicationRequest;
import com.digitalbank.accountopening.application.dto.UpdateApplicationRequest;
import com.digitalbank.accountopening.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class AccountApplicationController {

    private final ApplicationService applicationService;

    public AccountApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationResponse>> createApplication(
            @Valid @RequestBody CreateApplicationRequest request
    ) {
        ApplicationResponse response = applicationService.createApplication(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Application created successfully", response));
    }

    @GetMapping("/{applicationId}")
    public ApiResponse<ApplicationResponse> getApplication(@PathVariable UUID applicationId) {
        return ApiResponse.success(
                "Application retrieved successfully",
                applicationService.getApplication(applicationId)
        );
    }

    @PatchMapping("/{applicationId}")
    public ApiResponse<ApplicationResponse> updateApplication(
            @PathVariable UUID applicationId,
            @Valid @RequestBody UpdateApplicationRequest request
    ) {
        return ApiResponse.success(
                "Application updated successfully",
                applicationService.updateApplication(applicationId, request)
        );
    }

    @PatchMapping("/{applicationId}/submit")
    public ApiResponse<ApplicationResponse> submitApplication(@PathVariable UUID applicationId) {
        return ApiResponse.success(
                "Application submitted successfully",
                applicationService.submitApplication(applicationId)
        );
    }

    @PatchMapping("/{applicationId}/cancel")
    public ApiResponse<ApplicationResponse> cancelApplication(@PathVariable UUID applicationId) {
        return ApiResponse.success(
                "Application cancelled successfully",
                applicationService.cancelApplication(applicationId)
        );
    }

    @GetMapping("/{applicationId}/history")
    public ApiResponse<List<ApplicationHistoryResponse>> getApplicationHistory(
            @PathVariable UUID applicationId
    ) {
        return ApiResponse.success(
                "Application history retrieved successfully",
                applicationService.getApplicationHistory(applicationId)
        );
    }

    @PostMapping("/{applicationId}/kyc-check")
    public ApiResponse<ApplicationKycVerificationResponse> verifyCifKyc(
            @PathVariable UUID applicationId
    ) {
        return ApiResponse.success(
                "CIF/KYC verification completed successfully",
                applicationService.verifyCifKyc(applicationId)
        );
    }
}
