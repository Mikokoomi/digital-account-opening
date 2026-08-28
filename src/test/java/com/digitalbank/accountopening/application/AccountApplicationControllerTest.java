package com.digitalbank.accountopening.application;

import com.digitalbank.accountopening.application.dto.ApplicationHistoryResponse;
import com.digitalbank.accountopening.application.dto.ApplicationKycVerificationResponse;
import com.digitalbank.accountopening.application.dto.ApplicationResponse;
import com.digitalbank.accountopening.application.dto.CreateApplicationRequest;
import com.digitalbank.accountopening.application.dto.UpdateApplicationRequest;
import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import com.digitalbank.accountopening.common.exception.ApplicationNotCancellableException;
import com.digitalbank.accountopening.common.exception.ApplicationNotEditableException;
import com.digitalbank.accountopening.common.exception.ApplicationNotFoundException;
import com.digitalbank.accountopening.common.exception.ApplicationNotSubmittableException;
import com.digitalbank.accountopening.common.exception.ApplicationKycCheckNotAllowedException;
import com.digitalbank.accountopening.common.exception.ApplicationRuleEvaluationNotAllowedException;
import com.digitalbank.accountopening.common.exception.ApplicationKycVerificationRequiredException;
import com.digitalbank.accountopening.common.exception.ApplicationMandatoryConditionsNotSatisfiedException;
import com.digitalbank.accountopening.common.exception.ApplicationProcessingNotAllowedException;
import com.digitalbank.accountopening.common.exception.KycAlreadyVerifiedException;
import com.digitalbank.accountopening.integration.cifkyc.CifKycClientException;
import com.digitalbank.accountopening.integration.cifkyc.CifKycVerificationErrorCode;
import com.digitalbank.accountopening.integration.cifkyc.CifKycVerificationException;
import com.digitalbank.accountopening.integration.cifkyc.ReviewReason;
import com.digitalbank.accountopening.application.dto.ApplicationRuleEvaluationResponse;
import com.digitalbank.accountopening.application.rule.ApplicationRuleCode;
import com.digitalbank.accountopening.application.rule.RuleResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountApplicationController.class)
class AccountApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplicationService applicationService;

    @Test
    void createApplication_shouldReturnBadRequestForEmptyFields() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "",
                                  "productCode": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void createApplication_shouldReturnCreatedApplication() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.createApplication(any(CreateApplicationRequest.class)))
                .thenReturn(response(applicationId, "CURRENT_ACCOUNT", ApplicationStatus.DRAFT, null));

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "CUSTOMER-001",
                                  "productCode": "CURRENT_ACCOUNT"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Application created successfully"))
                .andExpect(jsonPath("$.data.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void updateApplication_shouldReturnUpdatedDraftApplication() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.updateApplication(eq(applicationId), any(UpdateApplicationRequest.class)))
                .thenReturn(response(applicationId, "SAVING_ACCOUNT", ApplicationStatus.DRAFT, null));

        mockMvc.perform(patch("/api/applications/{applicationId}", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "productCode": "SAVING_ACCOUNT" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Application updated successfully"))
                .andExpect(jsonPath("$.data.productCode").value("SAVING_ACCOUNT"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void updateApplication_shouldReturnBadRequestForEmptyProductCode() throws Exception {
        mockMvc.perform(patch("/api/applications/{applicationId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "productCode": "" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void submitApplication_shouldReturnSubmittedApplication() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.submitApplication(applicationId))
                .thenReturn(response(applicationId, "CURRENT_ACCOUNT", ApplicationStatus.SUBMITTED, OffsetDateTime.now()));

        mockMvc.perform(patch("/api/applications/{applicationId}/submit", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Application submitted successfully"))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.submittedAt").isNotEmpty());
    }

    @Test
    void submitApplication_shouldReturnConflictWhenApplicationIsNotSubmittable() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.submitApplication(applicationId))
                .thenThrow(new ApplicationNotSubmittableException());

        mockMvc.perform(patch("/api/applications/{applicationId}/submit", applicationId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("APPLICATION_NOT_SUBMITTABLE"));
    }

    @Test
    void updateApplication_shouldReturnConflictWhenApplicationIsNotEditable() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.updateApplication(eq(applicationId), any(UpdateApplicationRequest.class)))
                .thenThrow(new ApplicationNotEditableException());

        mockMvc.perform(patch("/api/applications/{applicationId}", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "productCode": "SAVING_ACCOUNT" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("APPLICATION_NOT_EDITABLE"));
    }

    @Test
    void cancelApplication_shouldReturnCancelledApplication() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.cancelApplication(applicationId))
                .thenReturn(response(
                        applicationId,
                        "CURRENT_ACCOUNT",
                        ApplicationStatus.CANCELLED,
                        null,
                        OffsetDateTime.now()
                ));

        mockMvc.perform(patch("/api/applications/{applicationId}/cancel", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Application cancelled successfully"))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelledAt").isNotEmpty());
    }

    @Test
    void cancelApplication_shouldReturnConflictWhenApplicationIsNotCancellable() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.cancelApplication(applicationId))
                .thenThrow(new ApplicationNotCancellableException());

        mockMvc.perform(patch("/api/applications/{applicationId}/cancel", applicationId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("APPLICATION_NOT_CANCELLABLE"));
    }

    @Test
    void getApplicationHistory_shouldReturnHistoryArray() throws Exception {
        UUID applicationId = UUID.randomUUID();
        OffsetDateTime firstChangedAt = OffsetDateTime.now().minusMinutes(1);
        OffsetDateTime secondChangedAt = OffsetDateTime.now();
        when(applicationService.getApplicationHistory(applicationId)).thenReturn(List.of(
                new ApplicationHistoryResponse(
                        UUID.randomUUID(),
                        null,
                        ApplicationStatus.DRAFT,
                        "CUSTOMER-001",
                        "Application created",
                        firstChangedAt
                ),
                new ApplicationHistoryResponse(
                        UUID.randomUUID(),
                        ApplicationStatus.DRAFT,
                        ApplicationStatus.CANCELLED,
                        "CUSTOMER-001",
                        "Application cancelled",
                        secondChangedAt
                )
        ));

        mockMvc.perform(get("/api/applications/{applicationId}/history", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].fromStatus").doesNotExist())
                .andExpect(jsonPath("$.data[0].toStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data[1].toStatus").value("CANCELLED"));
    }

    @Test
    void getApplicationHistory_shouldReturnNotFoundWhenApplicationDoesNotExist() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.getApplicationHistory(applicationId))
                .thenThrow(new ApplicationNotFoundException(applicationId));

        mockMvc.perform(get("/api/applications/{applicationId}/history", applicationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("APPLICATION_NOT_FOUND"));
    }

    @Test
    void getApplicationHistory_shouldReturnBadRequestForInvalidUuid() throws Exception {
        mockMvc.perform(get("/api/applications/{applicationId}/history", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_UUID"));
    }

    @Test
    void verifyCifKyc_shouldReturnEligibleResult() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.verifyCifKyc(applicationId)).thenReturn(
                new ApplicationKycVerificationResponse(
                        applicationId,
                        "CUS001",
                        true,
                        "ACTIVE",
                        "VERIFIED",
                        LocalDate.of(2027, 12, 31),
                        false,
                        null
                )
        );

        mockMvc.perform(post("/api/applications/{applicationId}/kyc-check", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("CIF/KYC verification completed successfully"))
                .andExpect(jsonPath("$.data.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.data.customerId").value("CUS001"))
                .andExpect(jsonPath("$.data.eligible").value(true))
                .andExpect(jsonPath("$.data.customerStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.kycStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.data.kycExpiryDate").value("2027-12-31"))
                .andExpect(jsonPath("$.data.reviewRequired").value(false))
                .andExpect(jsonPath("$.data.reviewReason").doesNotExist());
    }

    @Test
    void verifyCifKyc_shouldReturnNotFoundWhenApplicationDoesNotExist() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.verifyCifKyc(applicationId))
                .thenThrow(new ApplicationNotFoundException(applicationId));

        mockMvc.perform(post("/api/applications/{applicationId}/kyc-check", applicationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("APPLICATION_NOT_FOUND"));
    }

    @Test
    void verifyCifKyc_shouldReturnNotFoundWhenCustomerDoesNotExist() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.verifyCifKyc(applicationId)).thenThrow(
                new CifKycVerificationException(CifKycVerificationErrorCode.CUSTOMER_NOT_FOUND)
        );

        mockMvc.perform(post("/api/applications/{applicationId}/kyc-check", applicationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CUSTOMER_NOT_FOUND"));
    }

    @Test
    void verifyCifKyc_shouldReturnConflictWhenCustomerIsNotActive() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.verifyCifKyc(applicationId)).thenThrow(
                new CifKycVerificationException(CifKycVerificationErrorCode.CUSTOMER_NOT_ACTIVE)
        );

        mockMvc.perform(post("/api/applications/{applicationId}/kyc-check", applicationId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CUSTOMER_NOT_ACTIVE"));
    }

    @Test
    void verifyCifKyc_shouldReturnConflictWhenKycIsNotVerified() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.verifyCifKyc(applicationId)).thenThrow(
                new CifKycVerificationException(CifKycVerificationErrorCode.KYC_NOT_VERIFIED)
        );

        mockMvc.perform(post("/api/applications/{applicationId}/kyc-check", applicationId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("KYC_NOT_VERIFIED"));
    }

    @Test
    void verifyCifKyc_shouldReturnConflictWhenKycIsExpired() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.verifyCifKyc(applicationId)).thenThrow(
                new CifKycVerificationException(CifKycVerificationErrorCode.KYC_EXPIRED)
        );

        mockMvc.perform(post("/api/applications/{applicationId}/kyc-check", applicationId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("KYC_EXPIRED"));
    }

    @Test
    void verifyCifKyc_shouldReturnServiceUnavailableForIntegrationFailure() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.verifyCifKyc(applicationId)).thenThrow(
                new CifKycClientException("CIF/KYC service returned HTTP 500")
        );

        mockMvc.perform(post("/api/applications/{applicationId}/kyc-check", applicationId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("CIF_KYC_SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("CIF/KYC service is unavailable"));
    }
    @Test
    void verifyCifKyc_shouldReturnConflictWhenAlreadyVerified() throws Exception {
        UUID applicationId = UUID.randomUUID();

        when(applicationService.verifyCifKyc(applicationId))
                .thenThrow(new KycAlreadyVerifiedException());

        mockMvc.perform(post("/api/applications/{applicationId}/kyc-check", applicationId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("KYC_ALREADY_VERIFIED"))
                .andExpect(jsonPath("$.message")
                        .value("Application already has a successful KYC verification"));
        }

    @Test
    void verifyCifKyc_shouldReturnConflictWhenApplicationStatusIsNotSubmitted() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.verifyCifKyc(applicationId)).thenThrow(
                new ApplicationKycCheckNotAllowedException(ApplicationStatus.DRAFT)
        );

        mockMvc.perform(post("/api/applications/{applicationId}/kyc-check", applicationId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("APPLICATION_KYC_CHECK_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message")
                        .value("KYC check is not allowed for application status: DRAFT"));
    }

    @Test
    void getApplication_shouldIncludePersistedKycExpiryDate() throws Exception {
        UUID applicationId = UUID.randomUUID();
        OffsetDateTime verifiedAt = OffsetDateTime.parse("2026-08-21T05:00:00Z");
        when(applicationService.getApplication(applicationId)).thenReturn(new ApplicationResponse(
                applicationId,
                "CUS001",
                "CURRENT_ACCOUNT",
                "Current Account",
                ApplicationStatus.SUBMITTED,
                "VERIFIED",
                verifiedAt,
                LocalDate.of(2027, 12, 31),
                false,
                null,
                null,
                null,
                null,
                verifiedAt,
                verifiedAt
        ));

        mockMvc.perform(get("/api/applications/{applicationId}", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kycStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.data.cifVerifiedAt").value("2026-08-21T05:00:00Z"))
                .andExpect(jsonPath("$.data.kycExpiryDate").value("2027-12-31"))
                .andExpect(jsonPath("$.data.reviewRequired").value(false));
    }

        @Test
        void evaluateRules_shouldReturnEligibleResult() throws Exception {
        UUID applicationId = UUID.randomUUID();

        List<RuleResult> ruleResults = List.of(
                new RuleResult(
                        ApplicationRuleCode.PRODUCT_ACTIVE,
                        true,
                        "Product is active"
                ),
                new RuleResult(
                        ApplicationRuleCode.KYC_VERIFIED,
                        true,
                        "KYC verification is confirmed"
                )
        );

        when(applicationService.evaluateRules(applicationId))
                .thenReturn(new ApplicationRuleEvaluationResponse(
                        applicationId,
                        true,
                        ruleResults,
                        List.of()
                ));

        mockMvc.perform(post(
                        "/api/applications/{applicationId}/evaluate-rules",
                        applicationId
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Application business rules evaluated successfully"))
                .andExpect(jsonPath("$.data.applicationId")
                        .value(applicationId.toString()))
                .andExpect(jsonPath("$.data.eligible").value(true))
                .andExpect(jsonPath("$.data.ruleResults").isArray())
                .andExpect(jsonPath("$.data.ruleResults.length()").value(2))
                .andExpect(jsonPath("$.data.failedRules").isEmpty());
        }
        @Test
void evaluateRules_shouldReturnNotEligibleResult() throws Exception {
    UUID applicationId = UUID.randomUUID();

    RuleResult productRule = new RuleResult(
            ApplicationRuleCode.PRODUCT_ACTIVE,
            true,
            "Product is active"
    );

    RuleResult kycRule = new RuleResult(
            ApplicationRuleCode.KYC_VERIFIED,
            false,
            "KYC has not been verified"
    );

    when(applicationService.evaluateRules(applicationId))
            .thenReturn(new ApplicationRuleEvaluationResponse(
                    applicationId,
                    false,
                    List.of(productRule, kycRule),
                    List.of(kycRule)
            ));

    mockMvc.perform(post(
                    "/api/applications/{applicationId}/evaluate-rules",
                    applicationId
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.eligible").value(false))
            .andExpect(jsonPath("$.data.failedRules.length()").value(1))
            .andExpect(jsonPath("$.data.failedRules[0].ruleCode")
                    .value("KYC_VERIFIED"))
            .andExpect(jsonPath("$.data.failedRules[0].passed")
                    .value(false));
}
@Test
void evaluateRules_shouldReturnNotFoundWhenApplicationDoesNotExist()
        throws Exception {

    UUID applicationId = UUID.randomUUID();

    when(applicationService.evaluateRules(applicationId))
            .thenThrow(new ApplicationNotFoundException(applicationId));

    mockMvc.perform(post(
                    "/api/applications/{applicationId}/evaluate-rules",
                    applicationId
            ))
            .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode")
                    .value("APPLICATION_NOT_FOUND"));
}

    @Test
    void evaluateRules_shouldReturnConflictWhenApplicationStatusIsNotAllowed() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.evaluateRules(applicationId)).thenThrow(
                new ApplicationRuleEvaluationNotAllowedException(ApplicationStatus.CANCELLED)
        );

        mockMvc.perform(post(
                        "/api/applications/{applicationId}/evaluate-rules",
                        applicationId
                ))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode")
                        .value("APPLICATION_RULE_EVALUATION_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value(
                        "Business rule evaluation is not allowed for application status: CANCELLED"
                ));
    }

    @Test
    void processApplication_shouldReturnProcessedApplication() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.processApplication(applicationId))
                .thenReturn(response(applicationId, "CURRENT_ACCOUNT", ApplicationStatus.APPROVED, null));

        mockMvc.perform(post("/api/applications/{applicationId}/process", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Application processed successfully"))
                .andExpect(jsonPath("$.data.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void processApplication_shouldReturnConflictWhenStatusIsNotSubmitted() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.processApplication(applicationId))
                .thenThrow(new ApplicationProcessingNotAllowedException(ApplicationStatus.DRAFT));

        mockMvc.perform(post("/api/applications/{applicationId}/process", applicationId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("APPLICATION_PROCESSING_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value(
                        "Application processing is not allowed for application status: DRAFT"
                ));
    }

    @Test
    void processApplication_shouldReturnConflictWhenCurrentKycVerificationIsMissing() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.processApplication(applicationId))
                .thenThrow(new ApplicationKycVerificationRequiredException());

        mockMvc.perform(post("/api/applications/{applicationId}/process", applicationId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("APPLICATION_KYC_VERIFICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value(
                        "Current KYC verification is required before processing application"
                ));
    }

    @Test
    void processApplication_shouldReturnConflictWhenMandatoryConditionsFail() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.processApplication(applicationId))
                .thenThrow(new ApplicationMandatoryConditionsNotSatisfiedException());

        mockMvc.perform(post("/api/applications/{applicationId}/process", applicationId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode")
                        .value("APPLICATION_MANDATORY_CONDITIONS_NOT_SATISFIED"));
    }

    @Test
    void processApplication_shouldReturnNotFoundWhenApplicationDoesNotExist() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(applicationService.processApplication(applicationId))
                .thenThrow(new ApplicationNotFoundException(applicationId));

        mockMvc.perform(post("/api/applications/{applicationId}/process", applicationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("APPLICATION_NOT_FOUND"));
    }

    private ApplicationResponse response(
            UUID applicationId,
            String productCode,
            ApplicationStatus status,
            OffsetDateTime submittedAt
    ) {
        return response(applicationId, productCode, status, submittedAt, null);
    }

    private ApplicationResponse response(
            UUID applicationId,
            String productCode,
            ApplicationStatus status,
            OffsetDateTime submittedAt,
            OffsetDateTime cancelledAt
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        return new ApplicationResponse(
                applicationId,
                "CUSTOMER-001",
                productCode,
                "Test Product",
                status,
                null,
                null,
                null,
                null,
                null,
                null,
                submittedAt,
                cancelledAt,
                now,
                now
        );
    }
}
