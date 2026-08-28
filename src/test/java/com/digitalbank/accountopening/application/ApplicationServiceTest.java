package com.digitalbank.accountopening.application;

import com.digitalbank.accountopening.application.dto.ApplicationResponse;
import com.digitalbank.accountopening.application.dto.ApplicationHistoryResponse;
import com.digitalbank.accountopening.application.dto.ApplicationKycVerificationResponse;
import com.digitalbank.accountopening.application.dto.CreateApplicationRequest;
import com.digitalbank.accountopening.application.dto.UpdateApplicationRequest;
import com.digitalbank.accountopening.application.enums.ApplicationStatus;

import com.digitalbank.accountopening.application.dto.ApplicationRuleEvaluationResponse;
import com.digitalbank.accountopening.application.rule.ApplicationRuleCode;
import com.digitalbank.accountopening.application.rule.RuleEvaluationResult;
import com.digitalbank.accountopening.application.rule.RuleResult;

import com.digitalbank.accountopening.common.exception.ApplicationNotEditableException;
import com.digitalbank.accountopening.common.exception.ApplicationNotCancellableException;
import com.digitalbank.accountopening.common.exception.ApplicationNotFoundException;
import com.digitalbank.accountopening.common.exception.ApplicationNotSubmittableException;
import com.digitalbank.accountopening.common.exception.ApplicationKycCheckNotAllowedException;
import com.digitalbank.accountopening.common.exception.ApplicationRuleEvaluationNotAllowedException;
import com.digitalbank.accountopening.common.exception.ApplicationKycVerificationRequiredException;
import com.digitalbank.accountopening.common.exception.ApplicationMandatoryConditionsNotSatisfiedException;
import com.digitalbank.accountopening.common.exception.ApplicationProcessingNotAllowedException;
import com.digitalbank.accountopening.common.exception.ProductInactiveException;
import com.digitalbank.accountopening.common.exception.ProductNotFoundException;
import com.digitalbank.accountopening.common.exception.KycAlreadyVerifiedException;
import com.digitalbank.accountopening.product.Product;
import com.digitalbank.accountopening.product.ProductRepository;
import com.digitalbank.accountopening.integration.cifkyc.CifKycClientException;
import com.digitalbank.accountopening.integration.cifkyc.CifKycVerificationErrorCode;
import com.digitalbank.accountopening.integration.cifkyc.CifKycVerificationException;
import com.digitalbank.accountopening.integration.cifkyc.CifKycVerificationResult;
import com.digitalbank.accountopening.integration.cifkyc.CifKycVerificationService;
import com.digitalbank.accountopening.integration.cifkyc.ReviewReason;

import com.digitalbank.accountopening.application.rule.ApplicationRuleEvaluationService;
import com.digitalbank.accountopening.application.workflow.ApplicationWorkflowService;
import com.digitalbank.accountopening.approval.ApprovalCaseService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    private static final Instant TEST_INSTANT = Instant.parse("2026-08-12T05:00:00Z");

    @Mock
    private AccountApplicationRepository applicationRepository;

    @Mock
    private ApplicationStatusHistoryRepository historyRepository;

    @Mock
    private ApplicationRuleEvaluationService applicationRuleEvaluationService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CifKycVerificationService cifKycVerificationService;

    @Mock
    private ApprovalCaseService approvalCaseService;

    private ApplicationWorkflowService applicationWorkflowService;

    private ApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationWorkflowService = spy(new ApplicationWorkflowService(historyRepository));
        applicationService = new ApplicationService(
        applicationRepository,
        historyRepository,
        productRepository,
        new AccountApplicationMapper(),
        cifKycVerificationService,
        applicationRuleEvaluationService,
        applicationWorkflowService,
        approvalCaseService,
        Clock.fixed(TEST_INSTANT, ZoneOffset.UTC)
);
    }

    @Test
    void createApplication_shouldPersistDraftAndInitialHistory() {
        Product product = activeProduct();
        AccountApplication savedApplication = application(
                UUID.randomUUID(),
                "CUSTOMER-001",
                "CURRENT_ACCOUNT"
        );
        when(productRepository.findByProductCode("CURRENT_ACCOUNT")).thenReturn(Optional.of(product));
        when(applicationRepository.save(any(AccountApplication.class))).thenReturn(savedApplication);

        ApplicationResponse result = applicationService.createApplication(
                new CreateApplicationRequest(" CUSTOMER-001 ", " CURRENT_ACCOUNT ")
        );

        ArgumentCaptor<AccountApplication> applicationCaptor = ArgumentCaptor.forClass(AccountApplication.class);
        ArgumentCaptor<ApplicationStatusHistory> historyCaptor = ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(applicationRepository).save(applicationCaptor.capture());
        verify(historyRepository).save(historyCaptor.capture());

        assertEquals("CUSTOMER-001", applicationCaptor.getValue().getCustomerId());
        assertEquals("CURRENT_ACCOUNT", applicationCaptor.getValue().getProductCode());
        assertEquals(ApplicationStatus.DRAFT, applicationCaptor.getValue().getStatus());
        assertEquals(savedApplication, historyCaptor.getValue().getApplication());
        assertEquals(null, historyCaptor.getValue().getFromStatus());
        assertEquals(ApplicationStatus.DRAFT, historyCaptor.getValue().getToStatus());
        assertEquals("CUSTOMER-001", historyCaptor.getValue().getChangedBy());
        assertEquals("Application created", historyCaptor.getValue().getReason());
        assertEquals(savedApplication.getApplicationId(), result.applicationId());
        assertEquals("Current Account", result.productName());
        assertEquals(ApplicationStatus.DRAFT, result.status());
    }

    @Test
    void createApplication_shouldThrowWhenProductDoesNotExist() {
        when(productRepository.findByProductCode("UNKNOWN")).thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> applicationService.createApplication(new CreateApplicationRequest("CUSTOMER-001", "UNKNOWN"))
        );

        assertEquals("Product not found: UNKNOWN", exception.getMessage());
        verify(applicationRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void createApplication_shouldThrowWhenProductIsInactive() {
        Product product = activeProduct();
        product.setActive(false);
        when(productRepository.findByProductCode("CURRENT_ACCOUNT")).thenReturn(Optional.of(product));

        ProductInactiveException exception = assertThrows(
                ProductInactiveException.class,
                () -> applicationService.createApplication(new CreateApplicationRequest("CUSTOMER-001", "CURRENT_ACCOUNT"))
        );

        assertEquals("Product is inactive: CURRENT_ACCOUNT", exception.getMessage());
        verify(applicationRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void getApplication_shouldReturnMappedApplication() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(productRepository.findByProductCode("CURRENT_ACCOUNT")).thenReturn(Optional.of(activeProduct()));

        ApplicationResponse result = applicationService.getApplication(applicationId);

        assertEquals(applicationId, result.applicationId());
        assertEquals("CUSTOMER-001", result.customerId());
        assertEquals("CURRENT_ACCOUNT", result.productCode());
        assertEquals("Current Account", result.productName());
        assertEquals(ApplicationStatus.DRAFT, result.status());
    }

    @Test
    void getApplication_shouldThrowWhenApplicationDoesNotExist() {
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        ApplicationNotFoundException exception = assertThrows(
                ApplicationNotFoundException.class,
                () -> applicationService.getApplication(applicationId)
        );

        assertEquals("Application not found: " + applicationId, exception.getMessage());
    }

    @Test
    void updateApplication_shouldUpdateProductForDraftApplication() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        Product savingProduct = activeProduct("SAVING_ACCOUNT", "Saving Account");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(productRepository.findByProductCode("SAVING_ACCOUNT")).thenReturn(Optional.of(savingProduct));

        ApplicationResponse result = applicationService.updateApplication(
                applicationId,
                new UpdateApplicationRequest(" SAVING_ACCOUNT ")
        );

        assertEquals("SAVING_ACCOUNT", application.getProductCode());
        assertEquals(ApplicationStatus.DRAFT, application.getStatus());
        assertEquals("SAVING_ACCOUNT", result.productCode());
        assertEquals("Saving Account", result.productName());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void updateApplication_shouldThrowWhenApplicationDoesNotExist() {
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThrows(
                ApplicationNotFoundException.class,
                () -> applicationService.updateApplication(
                        applicationId,
                        new UpdateApplicationRequest("SAVING_ACCOUNT")
                )
        );
    }

    @Test
    void updateApplication_shouldThrowWhenApplicationIsNotDraft() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        application.setStatus(ApplicationStatus.SUBMITTED);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThrows(
                ApplicationNotEditableException.class,
                () -> applicationService.updateApplication(
                        applicationId,
                        new UpdateApplicationRequest("SAVING_ACCOUNT")
                )
        );

        assertEquals("CURRENT_ACCOUNT", application.getProductCode());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void updateApplication_shouldThrowWhenProductDoesNotExist() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(productRepository.findByProductCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> applicationService.updateApplication(applicationId, new UpdateApplicationRequest("UNKNOWN"))
        );
    }

    @Test
    void updateApplication_shouldThrowWhenProductIsInactive() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        Product product = activeProduct("SAVING_ACCOUNT", "Saving Account");
        product.setActive(false);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(productRepository.findByProductCode("SAVING_ACCOUNT")).thenReturn(Optional.of(product));

        assertThrows(
                ProductInactiveException.class,
                () -> applicationService.updateApplication(
                        applicationId,
                        new UpdateApplicationRequest("SAVING_ACCOUNT")
                )
        );

        assertEquals("CURRENT_ACCOUNT", application.getProductCode());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void submitApplication_shouldTransitionDraftToSubmittedAndPersistHistory() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(productRepository.findByProductCode("CURRENT_ACCOUNT")).thenReturn(Optional.of(activeProduct()));

        ApplicationResponse result = applicationService.submitApplication(applicationId);

        ArgumentCaptor<ApplicationStatusHistory> historyCaptor = ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertEquals(ApplicationStatus.SUBMITTED, application.getStatus());
        assertEquals(OffsetDateTime.ofInstant(TEST_INSTANT, ZoneOffset.UTC), application.getSubmittedAt());
        assertEquals(ApplicationStatus.SUBMITTED, result.status());
        assertEquals(OffsetDateTime.ofInstant(TEST_INSTANT, ZoneOffset.UTC), result.submittedAt());
        assertEquals(application, historyCaptor.getValue().getApplication());
        assertEquals(ApplicationStatus.DRAFT, historyCaptor.getValue().getFromStatus());
        assertEquals(ApplicationStatus.SUBMITTED, historyCaptor.getValue().getToStatus());
        assertEquals("CUSTOMER-001", historyCaptor.getValue().getChangedBy());
        assertEquals("Application submitted", historyCaptor.getValue().getReason());
        verify(applicationWorkflowService).transition(
                application,
                ApplicationStatus.SUBMITTED,
                "CUSTOMER-001",
                "Application submitted"
        );
    }

    @Test
    void submitApplication_shouldThrowWhenApplicationIsNotDraft() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        application.setStatus(ApplicationStatus.SUBMITTED);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThrows(
                ApplicationNotSubmittableException.class,
                () -> applicationService.submitApplication(applicationId)
        );

        verify(historyRepository, never()).save(any());
    }

    @Test
    void submitApplication_shouldThrowWhenApplicationDoesNotExist() {
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThrows(ApplicationNotFoundException.class, () -> applicationService.submitApplication(applicationId));
    }

    @Test
    void submitApplication_shouldThrowWhenProductIsInactive() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        Product product = activeProduct();
        product.setActive(false);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(productRepository.findByProductCode("CURRENT_ACCOUNT")).thenReturn(Optional.of(product));

        assertThrows(ProductInactiveException.class, () -> applicationService.submitApplication(applicationId));

        assertEquals(ApplicationStatus.DRAFT, application.getStatus());
        assertNull(application.getSubmittedAt());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void createApplication_shouldPropagateHistoryPersistenceFailure() {
        Product product = activeProduct();
        AccountApplication savedApplication = application(
                UUID.randomUUID(),
                "CUSTOMER-001",
                "CURRENT_ACCOUNT"
        );
        when(productRepository.findByProductCode("CURRENT_ACCOUNT")).thenReturn(Optional.of(product));
        when(applicationRepository.save(any(AccountApplication.class))).thenReturn(savedApplication);
        doThrow(new IllegalStateException("History persistence failed"))
                .when(historyRepository)
                .save(any(ApplicationStatusHistory.class));

        assertThrows(
                IllegalStateException.class,
                () -> applicationService.createApplication(
                        new CreateApplicationRequest("CUSTOMER-001", "CURRENT_ACCOUNT")
                )
        );
    }

    @Test
    void cancelApplication_shouldCancelDraftAndPersistHistory() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(productRepository.findByProductCode("CURRENT_ACCOUNT")).thenReturn(Optional.of(activeProduct()));

        ApplicationResponse result = applicationService.cancelApplication(applicationId);

        ArgumentCaptor<ApplicationStatusHistory> historyCaptor = ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertEquals(ApplicationStatus.CANCELLED, application.getStatus());
        assertEquals(OffsetDateTime.ofInstant(TEST_INSTANT, ZoneOffset.UTC), application.getCancelledAt());
        assertEquals(ApplicationStatus.CANCELLED, result.status());
        assertEquals(OffsetDateTime.ofInstant(TEST_INSTANT, ZoneOffset.UTC), result.cancelledAt());
        assertEquals(ApplicationStatus.DRAFT, historyCaptor.getValue().getFromStatus());
        assertEquals(ApplicationStatus.CANCELLED, historyCaptor.getValue().getToStatus());
        assertEquals("CUSTOMER-001", historyCaptor.getValue().getChangedBy());
        assertEquals("Application cancelled", historyCaptor.getValue().getReason());
        verify(applicationWorkflowService).transition(
                application,
                ApplicationStatus.CANCELLED,
                "CUSTOMER-001",
                "Application cancelled"
        );
    }

    @Test
    void cancelApplication_shouldCancelSubmittedAndKeepSubmittedAt() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        OffsetDateTime submittedAt = OffsetDateTime.now().minusMinutes(1);
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setSubmittedAt(submittedAt);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(productRepository.findByProductCode("CURRENT_ACCOUNT")).thenReturn(Optional.of(activeProduct()));

        applicationService.cancelApplication(applicationId);

        ArgumentCaptor<ApplicationStatusHistory> historyCaptor = ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertEquals(ApplicationStatus.CANCELLED, application.getStatus());
        assertEquals(submittedAt, application.getSubmittedAt());
        assertEquals(OffsetDateTime.ofInstant(TEST_INSTANT, ZoneOffset.UTC), application.getCancelledAt());
        assertEquals(ApplicationStatus.SUBMITTED, historyCaptor.getValue().getFromStatus());
        assertEquals(ApplicationStatus.CANCELLED, historyCaptor.getValue().getToStatus());
    }

    @Test
    void cancelApplication_shouldNotRequireAnActiveProduct() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        Product inactiveProduct = activeProduct();
        inactiveProduct.setActive(false);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(productRepository.findByProductCode("CURRENT_ACCOUNT")).thenReturn(Optional.of(inactiveProduct));

        ApplicationResponse result = applicationService.cancelApplication(applicationId);

        assertEquals(ApplicationStatus.CANCELLED, result.status());
        verify(historyRepository).save(any(ApplicationStatusHistory.class));
    }

    @Test
    void cancelApplication_shouldThrowWhenApplicationDoesNotExist() {
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThrows(ApplicationNotFoundException.class, () -> applicationService.cancelApplication(applicationId));

        verify(historyRepository, never()).save(any());
    }

    @Test
    void cancelApplication_shouldThrowWhenApplicationIsAlreadyCancelled() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        application.setStatus(ApplicationStatus.CANCELLED);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThrows(ApplicationNotCancellableException.class, () -> applicationService.cancelApplication(applicationId));

        verify(historyRepository, never()).save(any());
    }

    @Test
    void cancelApplication_shouldThrowWhenApplicationIsApproved() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        application.setStatus(ApplicationStatus.APPROVED);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThrows(ApplicationNotCancellableException.class, () -> applicationService.cancelApplication(applicationId));

        verify(historyRepository, never()).save(any());
    }

    @Test
    void getApplicationHistory_shouldReturnMappedHistoryInRepositoryOrder() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        OffsetDateTime firstChangedAt = OffsetDateTime.now().minusMinutes(2);
        OffsetDateTime secondChangedAt = OffsetDateTime.now().minusMinutes(1);
        ApplicationStatusHistory createdHistory = history(
                UUID.randomUUID(),
                application,
                null,
                ApplicationStatus.DRAFT,
                "Application created",
                firstChangedAt
        );
        ApplicationStatusHistory submittedHistory = history(
                UUID.randomUUID(),
                application,
                ApplicationStatus.DRAFT,
                ApplicationStatus.SUBMITTED,
                "Application submitted",
                secondChangedAt
        );
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(historyRepository.findAllByApplicationApplicationIdOrderByChangedAtAsc(applicationId))
                .thenReturn(List.of(createdHistory, submittedHistory));

        List<ApplicationHistoryResponse> result = applicationService.getApplicationHistory(applicationId);

        assertEquals(2, result.size());
        assertNull(result.getFirst().fromStatus());
        assertEquals(ApplicationStatus.DRAFT, result.getFirst().toStatus());
        assertEquals("Application created", result.getFirst().reason());
        assertEquals(firstChangedAt, result.getFirst().changedAt());
        assertEquals(ApplicationStatus.DRAFT, result.get(1).fromStatus());
        assertEquals(ApplicationStatus.SUBMITTED, result.get(1).toStatus());
        assertEquals(secondChangedAt, result.get(1).changedAt());
    }

    @Test
    void getApplicationHistory_shouldThrowBeforeLoadingHistoryWhenApplicationDoesNotExist() {
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThrows(ApplicationNotFoundException.class, () -> applicationService.getApplicationHistory(applicationId));

        verify(historyRepository, never()).findAllByApplicationApplicationIdOrderByChangedAtAsc(any());
    }

    @Test
    void verifyCifKyc_shouldUseCustomerIdFromApplicationAndReturnResult() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUS001", "CURRENT_ACCOUNT");
        application.setStatus(ApplicationStatus.SUBMITTED);
        CifKycVerificationResult verificationResult = verificationResult("CUS001");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(cifKycVerificationService.verify("CUS001")).thenReturn(verificationResult);

        ApplicationKycVerificationResponse result = applicationService.verifyCifKyc(applicationId);

        assertEquals(applicationId, result.applicationId());
        assertEquals("CUS001", result.customerId());
        assertTrue(result.eligible());
        assertEquals("ACTIVE", result.customerStatus());
        assertEquals("VERIFIED", result.kycStatus());
        assertEquals(LocalDate.of(2027, 12, 31), result.kycExpiryDate());
        assertEquals(false, result.reviewRequired());
        assertNull(result.reviewReason());
        assertEquals(ApplicationStatus.SUBMITTED, application.getStatus());
        assertEquals("VERIFIED", application.getKycStatus());
        assertEquals(OffsetDateTime.ofInstant(TEST_INSTANT, ZoneOffset.UTC), application.getCifVerifiedAt());
        assertEquals(LocalDate.of(2027, 12, 31), application.getKycExpiryDate());
        assertEquals(false, application.getReviewRequired());
        assertNull(application.getReviewReason());
        verify(cifKycVerificationService).verify("CUS001");
        verify(applicationRepository).save(application);
        verify(historyRepository, never()).save(any());
    }

    @Test
    void verifyCifKyc_shouldThrowWhenApplicationDoesNotExist() {
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThrows(
                ApplicationNotFoundException.class,
                () -> applicationService.verifyCifKyc(applicationId)
        );

        verifyNoInteractions(cifKycVerificationService);
    }

    @Test
    void verifyCifKyc_shouldPropagateCustomerNotFound() {
        assertVerificationFailure(CifKycVerificationErrorCode.CUSTOMER_NOT_FOUND);
    }

    @Test
    void verifyCifKyc_shouldPropagateCustomerNotActive() {
        assertVerificationFailure(CifKycVerificationErrorCode.CUSTOMER_NOT_ACTIVE);
    }

    @Test
    void verifyCifKyc_shouldPropagateKycNotVerified() {
        assertVerificationFailure(CifKycVerificationErrorCode.KYC_NOT_VERIFIED);
    }

    @Test
    void verifyCifKyc_shouldPropagateKycExpired() {
        assertVerificationFailure(CifKycVerificationErrorCode.KYC_EXPIRED);
    }

    @Test
    void verifyCifKyc_shouldPropagateIntegrationFailure() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUS001", "CURRENT_ACCOUNT");
        application.setStatus(ApplicationStatus.SUBMITTED);
        CifKycClientException clientException = new CifKycClientException(
                "CIF/KYC service is unavailable"
        );
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(cifKycVerificationService.verify("CUS001")).thenThrow(clientException);

        CifKycClientException thrownException = assertThrows(
                CifKycClientException.class,
                () -> applicationService.verifyCifKyc(applicationId)
        );

        assertSame(clientException, thrownException);
        assertEquals(ApplicationStatus.SUBMITTED, application.getStatus());
        assertNull(application.getKycStatus());
        assertNull(application.getCifVerifiedAt());
        assertNull(application.getKycExpiryDate());
        verify(applicationRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    private void assertVerificationFailure(CifKycVerificationErrorCode errorCode) {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUS001", "CURRENT_ACCOUNT");
        application.setStatus(ApplicationStatus.SUBMITTED);
        CifKycVerificationException verificationException = new CifKycVerificationException(errorCode);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(cifKycVerificationService.verify("CUS001")).thenThrow(verificationException);

        CifKycVerificationException thrownException = assertThrows(
                CifKycVerificationException.class,
                () -> applicationService.verifyCifKyc(applicationId)
        );

        assertSame(verificationException, thrownException);
        assertEquals(ApplicationStatus.SUBMITTED, application.getStatus());
        assertNull(application.getKycStatus());
        assertNull(application.getCifVerifiedAt());
        assertNull(application.getKycExpiryDate());
        verify(applicationRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void verifyCifKyc_shouldRejectWhenApplicationIsAlreadyVerified() {
        UUID applicationId = UUID.randomUUID();

        AccountApplication application = application(
                applicationId,
                "CUSTOMER-001",
                "CURRENT_ACCOUNT"
        );

        application.setKycStatus("VERIFIED");
        application.setCifVerifiedAt(
                OffsetDateTime.ofInstant(TEST_INSTANT, ZoneOffset.UTC)
        );
        application.setKycExpiryDate(LocalDate.of(2027, 12, 31));
        application.setStatus(ApplicationStatus.SUBMITTED);

        when(applicationRepository.findById(applicationId))
                .thenReturn(Optional.of(application));

        KycAlreadyVerifiedException exception = assertThrows(
                KycAlreadyVerifiedException.class,
                () -> applicationService.verifyCifKyc(applicationId)
        );

    assertEquals(
            "Application already has a successful KYC verification",
            exception.getMessage()
    );

    verifyNoInteractions(cifKycVerificationService);
    verify(applicationRepository, never()).save(any());
}

    @Test
    void verifyCifKyc_shouldRejectStatusesOutsideSubmitted() {
        for (ApplicationStatus status : List.of(
                ApplicationStatus.DRAFT,
                ApplicationStatus.UNDER_REVIEW,
                ApplicationStatus.APPROVED,
                ApplicationStatus.REJECTED,
                ApplicationStatus.CANCELLED,
                ApplicationStatus.FAILED
        )) {
            UUID applicationId = UUID.randomUUID();
            AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
            application.setStatus(status);
            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

            ApplicationKycCheckNotAllowedException exception = assertThrows(
                    ApplicationKycCheckNotAllowedException.class,
                    () -> applicationService.verifyCifKyc(applicationId)
            );

            assertEquals("KYC check is not allowed for application status: " + status, exception.getMessage());
        }

        verifyNoInteractions(cifKycVerificationService);
        verify(applicationRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void verifyCifKyc_shouldPersistManualReviewSnapshot() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUS002", "CURRENT_ACCOUNT");
        application.setStatus(ApplicationStatus.SUBMITTED);
        CifKycVerificationResult result = new CifKycVerificationResult(
                "CUS002",
                true,
                "ACTIVE",
                "VERIFIED",
                LocalDate.of(2027, 12, 31),
                true,
                ReviewReason.CUSTOMER_PROFILE_REVIEW
        );
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(cifKycVerificationService.verify("CUS002")).thenReturn(result);

        ApplicationKycVerificationResponse response = applicationService.verifyCifKyc(applicationId);

        assertTrue(response.reviewRequired());
        assertEquals(ReviewReason.CUSTOMER_PROFILE_REVIEW, response.reviewReason());
        assertEquals(true, application.getReviewRequired());
        assertEquals(ReviewReason.CUSTOMER_PROFILE_REVIEW, application.getReviewReason());
        verify(applicationRepository).save(application);
    }

    @Test
    void verifyCifKyc_shouldTreatExpiryTodayAsAlreadyVerified() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setKycStatus("VERIFIED");
        application.setCifVerifiedAt(OffsetDateTime.ofInstant(TEST_INSTANT, ZoneOffset.UTC));
        application.setKycExpiryDate(LocalDate.of(2026, 8, 12));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThrows(KycAlreadyVerifiedException.class, () -> applicationService.verifyCifKyc(applicationId));

        verifyNoInteractions(cifKycVerificationService);
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void verifyCifKyc_shouldAllowExpiredVerificationAndRefreshSnapshot() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setKycStatus("VERIFIED");
        application.setCifVerifiedAt(OffsetDateTime.parse("2026-08-01T05:00:00Z"));
        application.setKycExpiryDate(LocalDate.of(2026, 8, 11));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(cifKycVerificationService.verify("CUSTOMER-001"))
                .thenReturn(verificationResult("CUSTOMER-001"));

        applicationService.verifyCifKyc(applicationId);

        assertEquals(OffsetDateTime.ofInstant(TEST_INSTANT, ZoneOffset.UTC), application.getCifVerifiedAt());
        assertEquals(LocalDate.of(2027, 12, 31), application.getKycExpiryDate());
        verify(cifKycVerificationService).verify("CUSTOMER-001");
        verify(applicationRepository).save(application);
    }
    private CifKycVerificationResult verificationResult(String customerId) {
        return new CifKycVerificationResult(
                customerId,
                true,
                "ACTIVE",
                "VERIFIED",
                LocalDate.of(2027, 12, 31),
                false,
                null
        );
    }

    private Product activeProduct() {
        return activeProduct("CURRENT_ACCOUNT", "Current Account");
    }

    private Product activeProduct(String productCode, String productName) {
        Product product = new Product();
        product.setProductCode(productCode);
        product.setProductName(productName);
        product.setActive(true);
        return product;
    }

    private ApplicationStatusHistory history(
            UUID historyId,
            AccountApplication application,
            ApplicationStatus fromStatus,
            ApplicationStatus toStatus,
            String reason,
            OffsetDateTime changedAt
    ) {
        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setHistoryId(historyId);
        history.setApplication(application);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setChangedBy(application.getCustomerId());
        history.setReason(reason);
        history.setChangedAt(changedAt);
        return history;
    }

    private AccountApplication application(UUID applicationId, String customerId, String productCode) {
        OffsetDateTime now = OffsetDateTime.now();
        AccountApplication application = new AccountApplication();
        application.setApplicationId(applicationId);
        application.setCustomerId(customerId);
        application.setProductCode(productCode);
        application.setStatus(ApplicationStatus.DRAFT);
        application.setReviewRequired(false);
        application.setReviewReason(null);
        application.setCreatedAt(now);
        application.setUpdatedAt(now);
        return application;
    }
    @Test
    void evaluateRules_shouldReturnEligibleWhenAllRulesPass() {
        UUID applicationId = UUID.randomUUID();

        AccountApplication application = application(
                applicationId,
                "CUSTOMER-001",
                "CURRENT_ACCOUNT"
        );
        application.setStatus(ApplicationStatus.SUBMITTED);

        RuleResult productRule = new RuleResult(
                ApplicationRuleCode.PRODUCT_ACTIVE,
                true,
                "Product is active"
        );

        RuleResult kycRule = new RuleResult(
                ApplicationRuleCode.KYC_VERIFIED,
                true,
                "KYC verification is confirmed"
        );

        RuleEvaluationResult evaluationResult =
                RuleEvaluationResult.from(List.of(productRule, kycRule));

        when(applicationRepository.findById(applicationId))
                .thenReturn(Optional.of(application));

        when(applicationRuleEvaluationService.evaluate(application))
                .thenReturn(evaluationResult);

        ApplicationRuleEvaluationResponse response =
                applicationService.evaluateRules(applicationId);

        assertEquals(applicationId, response.applicationId());
        assertTrue(response.eligible());
        assertEquals(2, response.ruleResults().size());
        assertTrue(response.failedRules().isEmpty());

        verify(applicationRuleEvaluationService).evaluate(application);
        verify(applicationRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }
    @Test
    void evaluateRules_shouldReturnNotEligibleWhenKycRuleFails() {
        UUID applicationId = UUID.randomUUID();

        AccountApplication application = application(
                applicationId,
                "CUSTOMER-001",
                "CURRENT_ACCOUNT"
        );
        application.setStatus(ApplicationStatus.SUBMITTED);

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

        RuleEvaluationResult evaluationResult =
                RuleEvaluationResult.from(List.of(productRule, kycRule));

        when(applicationRepository.findById(applicationId))
                .thenReturn(Optional.of(application));

        when(applicationRuleEvaluationService.evaluate(application))
                .thenReturn(evaluationResult);

        ApplicationRuleEvaluationResponse response =
                applicationService.evaluateRules(applicationId);

        assertEquals(applicationId, response.applicationId());
        assertEquals(false, response.eligible());

        assertEquals(2, response.ruleResults().size());
        assertEquals(1, response.failedRules().size());

        assertEquals(
                ApplicationRuleCode.KYC_VERIFIED,
                response.failedRules().getFirst().ruleCode()
        );

        verify(applicationRuleEvaluationService).evaluate(application);
        verify(applicationRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void evaluateRules_shouldAllowSubmittedApplication() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        application.setStatus(ApplicationStatus.SUBMITTED);

        RuleEvaluationResult evaluationResult = RuleEvaluationResult.from(List.of(
                new RuleResult(ApplicationRuleCode.PRODUCT_ACTIVE, true, "Product is active"),
                new RuleResult(ApplicationRuleCode.KYC_VERIFIED, true, "KYC verification is confirmed")
        ));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(applicationRuleEvaluationService.evaluate(application)).thenReturn(evaluationResult);

        ApplicationRuleEvaluationResponse response = applicationService.evaluateRules(applicationId);

        assertTrue(response.eligible());
        assertEquals(ApplicationStatus.SUBMITTED, application.getStatus());
        verify(applicationRuleEvaluationService).evaluate(application);
        verify(applicationRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void evaluateRules_shouldRejectStatusesOutsideSubmitted() {
        for (ApplicationStatus status : List.of(
                ApplicationStatus.DRAFT,
                ApplicationStatus.UNDER_REVIEW,
                ApplicationStatus.APPROVED,
                ApplicationStatus.REJECTED,
                ApplicationStatus.CANCELLED,
                ApplicationStatus.FAILED
        )) {
            UUID applicationId = UUID.randomUUID();
            AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
            application.setStatus(status);
            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

            ApplicationRuleEvaluationNotAllowedException exception = assertThrows(
                    ApplicationRuleEvaluationNotAllowedException.class,
                    () -> applicationService.evaluateRules(applicationId)
            );

            assertEquals(
                    "Business rule evaluation is not allowed for application status: " + status,
                    exception.getMessage()
            );
            assertEquals(status, application.getStatus());
        }

        verifyNoInteractions(applicationRuleEvaluationService);
        verify(applicationRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void evaluateRules_shouldRejectNullStatus() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        application.setStatus(null);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        ApplicationRuleEvaluationNotAllowedException exception = assertThrows(
                ApplicationRuleEvaluationNotAllowedException.class,
                () -> applicationService.evaluateRules(applicationId)
        );

        assertEquals("Business rule evaluation is not allowed for application status: null", exception.getMessage());
        verifyNoInteractions(applicationRuleEvaluationService);
        verify(applicationRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void processApplication_shouldApproveEligibleSubmittedApplicationWithCurrentKyc() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setKycStatus("VERIFIED");
        application.setCifVerifiedAt(OffsetDateTime.ofInstant(TEST_INSTANT, ZoneOffset.UTC));
        application.setKycExpiryDate(LocalDate.of(2026, 8, 12));
        RuleEvaluationResult evaluationResult = RuleEvaluationResult.from(List.of(
                new RuleResult(ApplicationRuleCode.PRODUCT_ACTIVE, true, "Product is active"),
                new RuleResult(ApplicationRuleCode.KYC_VERIFIED, true, "KYC verification is confirmed")
        ));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(applicationRuleEvaluationService.evaluate(application)).thenReturn(evaluationResult);
        when(productRepository.findByProductCode("CURRENT_ACCOUNT")).thenReturn(Optional.of(activeProduct()));

        ApplicationResponse response = applicationService.processApplication(applicationId);

        ArgumentCaptor<ApplicationStatusHistory> historyCaptor =
                ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertEquals(ApplicationStatus.APPROVED, application.getStatus());
        assertEquals(ApplicationStatus.APPROVED, response.status());
        assertEquals(ApplicationStatus.SUBMITTED, historyCaptor.getValue().getFromStatus());
        assertEquals(ApplicationStatus.APPROVED, historyCaptor.getValue().getToStatus());
        assertEquals("SYSTEM", historyCaptor.getValue().getChangedBy());
        assertEquals(
                "Application automatically approved after mandatory business checks",
                historyCaptor.getValue().getReason()
        );
        verify(applicationRuleEvaluationService).evaluate(application);
        verifyNoInteractions(approvalCaseService);
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void processApplication_shouldBlockMandatoryRuleFailureWithoutApprovalCase() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setKycStatus("VERIFIED");
        application.setCifVerifiedAt(OffsetDateTime.ofInstant(TEST_INSTANT, ZoneOffset.UTC));
        application.setKycExpiryDate(LocalDate.of(2026, 8, 12));
        RuleEvaluationResult evaluationResult = RuleEvaluationResult.from(List.of(
                new RuleResult(ApplicationRuleCode.PRODUCT_ACTIVE, false, "Product is inactive"),
                new RuleResult(ApplicationRuleCode.KYC_VERIFIED, true, "KYC verification is confirmed")
        ));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(applicationRuleEvaluationService.evaluate(application)).thenReturn(evaluationResult);
        assertThrows(
                ApplicationMandatoryConditionsNotSatisfiedException.class,
                () -> applicationService.processApplication(applicationId)
        );

        assertEquals(ApplicationStatus.SUBMITTED, application.getStatus());
        verify(applicationRuleEvaluationService).evaluate(application);
        verifyNoInteractions(approvalCaseService);
        verify(historyRepository, never()).save(any());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void processApplication_shouldRouteValidReviewSignalToUnderReviewAndCreateCase() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUS002", "CURRENT_ACCOUNT");
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setKycStatus("VERIFIED");
        application.setCifVerifiedAt(OffsetDateTime.ofInstant(TEST_INSTANT, ZoneOffset.UTC));
        application.setKycExpiryDate(LocalDate.of(2026, 8, 12));
        application.setReviewRequired(true);
        application.setReviewReason(ReviewReason.CUSTOMER_PROFILE_REVIEW);
        RuleEvaluationResult evaluationResult = RuleEvaluationResult.from(List.of(
                new RuleResult(ApplicationRuleCode.PRODUCT_ACTIVE, true, "Product is active"),
                new RuleResult(ApplicationRuleCode.KYC_VERIFIED, true, "KYC verification is confirmed")
        ));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(applicationRuleEvaluationService.evaluate(application)).thenReturn(evaluationResult);
        when(productRepository.findByProductCode("CURRENT_ACCOUNT")).thenReturn(Optional.of(activeProduct()));

        ApplicationResponse response = applicationService.processApplication(applicationId);

        assertEquals(ApplicationStatus.UNDER_REVIEW, response.status());
        verify(approvalCaseService).createForManualReview(
                application,
                "CUSTOMER_PROFILE_REVIEW"
        );
        verify(historyRepository).save(any(ApplicationStatusHistory.class));
    }

    @Test
    void processApplication_shouldRejectStatusesOtherThanSubmittedWithoutEvaluationOrHistory() {
        for (ApplicationStatus status : List.of(
                ApplicationStatus.DRAFT,
                ApplicationStatus.UNDER_REVIEW,
                ApplicationStatus.APPROVED,
                ApplicationStatus.REJECTED,
                ApplicationStatus.CANCELLED,
                ApplicationStatus.FAILED
        )) {
            UUID applicationId = UUID.randomUUID();
            AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
            application.setStatus(status);
            when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

            ApplicationProcessingNotAllowedException exception = assertThrows(
                    ApplicationProcessingNotAllowedException.class,
                    () -> applicationService.processApplication(applicationId)
            );

            assertEquals(
                    "Application processing is not allowed for application status: " + status,
                    exception.getMessage()
            );
            assertEquals(status, application.getStatus());
        }

        verifyNoInteractions(applicationRuleEvaluationService);
        verifyNoInteractions(approvalCaseService);
        verify(historyRepository, never()).save(any());
    }

    @Test
    void processApplication_shouldRequireCurrentKycVerificationBeforeEvaluation() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setKycStatus("VERIFIED");
        application.setCifVerifiedAt(OffsetDateTime.ofInstant(TEST_INSTANT, ZoneOffset.UTC));
        application.setKycExpiryDate(LocalDate.of(2026, 8, 11));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        ApplicationKycVerificationRequiredException exception = assertThrows(
                ApplicationKycVerificationRequiredException.class,
                () -> applicationService.processApplication(applicationId)
        );

        assertEquals("Current KYC verification is required before processing application", exception.getMessage());
        assertEquals(ApplicationStatus.SUBMITTED, application.getStatus());
        verifyNoInteractions(applicationRuleEvaluationService);
        verify(historyRepository, never()).save(any());
        verify(applicationRepository, never()).save(any());
    }
}
