package com.digitalbank.accountopening.approval;

import com.digitalbank.accountopening.application.AccountApplication;
import com.digitalbank.accountopening.application.ApplicationStatusHistoryRepository;
import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import com.digitalbank.accountopening.application.rule.ApplicationRuleEvaluationService;
import com.digitalbank.accountopening.application.rule.RuleEvaluationResult;
import com.digitalbank.accountopening.application.rule.RuleResult;
import com.digitalbank.accountopening.application.rule.ApplicationRuleCode;
import com.digitalbank.accountopening.integration.cifkyc.ReviewReason;
import com.digitalbank.accountopening.application.workflow.ApplicationWorkflowService;
import com.digitalbank.accountopening.approval.dto.ApprovalCaseResponse;
import com.digitalbank.accountopening.common.exception.ApplicationNotUnderReviewException;
import com.digitalbank.accountopening.common.exception.ManualReviewDataInvalidException;
import com.digitalbank.accountopening.common.exception.ApprovalCaseAlreadyExistsException;
import com.digitalbank.accountopening.common.exception.ApprovalCaseBusinessRulesNotSatisfiedException;
import com.digitalbank.accountopening.common.exception.ApprovalCaseAssignmentNotAllowedException;
import com.digitalbank.accountopening.common.exception.ApprovalCaseDecisionNotAllowedException;
import com.digitalbank.accountopening.common.exception.ApprovalCaseNotFoundException;
import com.digitalbank.accountopening.common.exception.ApprovalCaseStaffMismatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalCaseServiceTest {

    private static final Instant TEST_INSTANT = Instant.parse("2026-08-28T02:00:00Z");

    @Mock
    private ApprovalCaseRepository approvalCaseRepository;

    @Mock
    private ApplicationStatusHistoryRepository historyRepository;

    @Mock
    private ApplicationRuleEvaluationService applicationRuleEvaluationService;

    private ApprovalCaseService approvalCaseService;

    @BeforeEach
    void setUp() {
        approvalCaseService = new ApprovalCaseService(
                approvalCaseRepository,
                new ApplicationWorkflowService(historyRepository),
                applicationRuleEvaluationService,
                Clock.fixed(TEST_INSTANT, ZoneOffset.UTC)
        );
    }

    @Test
    void createForManualReview_shouldCreatePendingCase() {
        AccountApplication application = application(ApplicationStatus.UNDER_REVIEW);
        when(approvalCaseRepository.existsByApplicationApplicationId(application.getApplicationId()))
                .thenReturn(false);
        when(approvalCaseRepository.saveAndFlush(any(ApprovalCase.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApprovalCase result = approvalCaseService.createForManualReview(application, "Rule failed");

        assertEquals(ApprovalCaseStatus.PENDING, result.getStatus());
        assertEquals(application, result.getApplication());
        assertEquals("Rule failed", result.getReviewReason());
        assertEquals(OffsetDateTime.ofInstant(TEST_INSTANT, ZoneOffset.UTC), result.getCreatedAt());
        assertEquals(result.getCreatedAt(), result.getUpdatedAt());
    }

    @Test
    void createForManualReview_shouldRejectDuplicateApplicationCase() {
        AccountApplication application = application(ApplicationStatus.UNDER_REVIEW);
        when(approvalCaseRepository.existsByApplicationApplicationId(application.getApplicationId()))
                .thenReturn(true);

        assertThrows(
                ApprovalCaseAlreadyExistsException.class,
                () -> approvalCaseService.createForManualReview(application, "Rule failed")
        );

        verify(approvalCaseRepository, never()).saveAndFlush(any());
    }

    @Test
    void createForManualReview_shouldRejectApplicationOutsideUnderReview() {
        AccountApplication application = application(ApplicationStatus.SUBMITTED);

        assertThrows(
                ApplicationNotUnderReviewException.class,
                () -> approvalCaseService.createForManualReview(application, "Rule failed")
        );

        verify(approvalCaseRepository, never()).saveAndFlush(any());
    }

    @Test
    void createForManualReview_shouldRejectMissingManualReviewSignal() {
        AccountApplication application = application(ApplicationStatus.UNDER_REVIEW);
        application.setReviewRequired(false);
        application.setReviewReason(null);

        assertThrows(
                ManualReviewDataInvalidException.class,
                () -> approvalCaseService.createForManualReview(application, "Invalid")
        );

        verify(approvalCaseRepository, never()).saveAndFlush(any());
    }

    @Test
    void assign_shouldMovePendingCaseToAssignedAndTrimStaffId() {
        ApprovalCase approvalCase = approvalCase(ApprovalCaseStatus.PENDING, ApplicationStatus.UNDER_REVIEW);
        when(approvalCaseRepository.findById(approvalCase.getCaseId()))
                .thenReturn(Optional.of(approvalCase));
        ApprovalCaseResponse response = approvalCaseService.assign(approvalCase.getCaseId(), " STAFF001 ");

        assertEquals(ApprovalCaseStatus.ASSIGNED, response.status());
        assertEquals("STAFF001", response.assignedTo());
        assertEquals(OffsetDateTime.ofInstant(TEST_INSTANT, ZoneOffset.UTC), response.assignedAt());
    }

    @Test
    void assign_shouldRejectTerminalCase() {
        ApprovalCase approvalCase = approvalCase(ApprovalCaseStatus.APPROVED, ApplicationStatus.APPROVED);
        when(approvalCaseRepository.findById(approvalCase.getCaseId()))
                .thenReturn(Optional.of(approvalCase));

        assertThrows(
                ApprovalCaseAssignmentNotAllowedException.class,
                () -> approvalCaseService.assign(approvalCase.getCaseId(), "STAFF001")
        );
    }

    @Test
    void getApprovalCase_shouldRejectMissingCase() {
        UUID caseId = UUID.randomUUID();
        when(approvalCaseRepository.findById(caseId)).thenReturn(Optional.empty());

        assertThrows(ApprovalCaseNotFoundException.class, () -> approvalCaseService.getApprovalCase(caseId));
    }

    @Test
    void approve_shouldCompleteCaseAndApplicationThroughWorkflow() {
        ApprovalCase approvalCase = assignedCase(ApplicationStatus.UNDER_REVIEW);
        when(approvalCaseRepository.findById(approvalCase.getCaseId()))
                .thenReturn(Optional.of(approvalCase));
        when(applicationRuleEvaluationService.evaluate(approvalCase.getApplication()))
                .thenReturn(eligibleResult());

        ApprovalCaseResponse response = approvalCaseService.approve(approvalCase.getCaseId(), " STAFF001 ");

        assertEquals(ApprovalCaseStatus.APPROVED, response.status());
        assertEquals(ApplicationStatus.APPROVED, response.applicationStatus());
        assertEquals(OffsetDateTime.ofInstant(TEST_INSTANT, ZoneOffset.UTC), response.decidedAt());
        assertNull(response.decisionReason());
        verify(historyRepository).save(argThat(argThatHistory(
                ApplicationStatus.UNDER_REVIEW,
                ApplicationStatus.APPROVED,
                "STAFF001",
                "Application approved by staff"
        )));
    }

    @Test
    void reject_shouldPersistReasonAndCompleteApplicationThroughWorkflow() {
        ApprovalCase approvalCase = assignedCase(ApplicationStatus.UNDER_REVIEW);
        when(approvalCaseRepository.findById(approvalCase.getCaseId()))
                .thenReturn(Optional.of(approvalCase));

        ApprovalCaseResponse response = approvalCaseService.reject(
                approvalCase.getCaseId(),
                "STAFF001",
                " Missing required evidence "
        );

        assertEquals(ApprovalCaseStatus.REJECTED, response.status());
        assertEquals(ApplicationStatus.REJECTED, response.applicationStatus());
        assertEquals("Missing required evidence", response.decisionReason());
        assertEquals("Missing required evidence", approvalCase.getApplication().getRejectReason());
        verify(historyRepository).save(argThat(argThatHistory(
                ApplicationStatus.UNDER_REVIEW,
                ApplicationStatus.REJECTED,
                "STAFF001",
                "Missing required evidence"
        )));
    }

    @Test
    void decision_shouldRejectUnassignedAndTerminalCases() {
        for (ApprovalCaseStatus status : new ApprovalCaseStatus[]{
                ApprovalCaseStatus.PENDING,
                ApprovalCaseStatus.APPROVED,
                ApprovalCaseStatus.REJECTED
        }) {
            ApprovalCase approvalCase = approvalCase(status, ApplicationStatus.UNDER_REVIEW);
            when(approvalCaseRepository.findById(approvalCase.getCaseId()))
                    .thenReturn(Optional.of(approvalCase));
            assertThrows(
                    ApprovalCaseDecisionNotAllowedException.class,
                    () -> approvalCaseService.approve(approvalCase.getCaseId(), "STAFF001")
            );
        }
        verify(historyRepository, never()).save(any());
    }

    @Test
    void decision_shouldRejectDifferentStaff() {
        ApprovalCase approvalCase = assignedCase(ApplicationStatus.UNDER_REVIEW);
        when(approvalCaseRepository.findById(approvalCase.getCaseId()))
                .thenReturn(Optional.of(approvalCase));

        assertThrows(
                ApprovalCaseStaffMismatchException.class,
                () -> approvalCaseService.approve(approvalCase.getCaseId(), "STAFF002")
        );
        verify(historyRepository, never()).save(any());
    }

    @Test
    void decision_shouldRejectApplicationOutsideUnderReview() {
        ApprovalCase approvalCase = assignedCase(ApplicationStatus.CANCELLED);
        when(approvalCaseRepository.findById(approvalCase.getCaseId()))
                .thenReturn(Optional.of(approvalCase));

        assertThrows(
                ApplicationNotUnderReviewException.class,
                () -> approvalCaseService.reject(approvalCase.getCaseId(), "STAFF001", "Reason")
        );
        verify(historyRepository, never()).save(any());
    }

    @Test
    void approve_shouldRejectWhenRequiredBusinessRulesNoLongerPass() {
        ApprovalCase approvalCase = assignedCase(ApplicationStatus.UNDER_REVIEW);
        when(approvalCaseRepository.findById(approvalCase.getCaseId()))
                .thenReturn(Optional.of(approvalCase));
        when(applicationRuleEvaluationService.evaluate(approvalCase.getApplication()))
                .thenReturn(RuleEvaluationResult.from(List.of(
                        new RuleResult(ApplicationRuleCode.PRODUCT_ACTIVE, false, "Product is inactive")
                )));

        assertThrows(
                ApprovalCaseBusinessRulesNotSatisfiedException.class,
                () -> approvalCaseService.approve(approvalCase.getCaseId(), "STAFF001")
        );

        assertEquals(ApplicationStatus.UNDER_REVIEW, approvalCase.getApplication().getStatus());
        assertEquals(ApprovalCaseStatus.ASSIGNED, approvalCase.getStatus());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void approve_shouldRejectWhenKycMandatoryRuleNoLongerPasses() {
        ApprovalCase approvalCase = assignedCase(ApplicationStatus.UNDER_REVIEW);
        when(approvalCaseRepository.findById(approvalCase.getCaseId()))
                .thenReturn(Optional.of(approvalCase));
        when(applicationRuleEvaluationService.evaluate(approvalCase.getApplication()))
                .thenReturn(RuleEvaluationResult.from(List.of(
                        new RuleResult(ApplicationRuleCode.PRODUCT_ACTIVE, true, "Product is active"),
                        new RuleResult(ApplicationRuleCode.KYC_VERIFIED, false, "KYC has expired")
                )));

        assertThrows(
                ApprovalCaseBusinessRulesNotSatisfiedException.class,
                () -> approvalCaseService.approve(approvalCase.getCaseId(), "STAFF001")
        );

        assertEquals(ApplicationStatus.UNDER_REVIEW, approvalCase.getApplication().getStatus());
        assertEquals(ApprovalCaseStatus.ASSIGNED, approvalCase.getStatus());
        verify(historyRepository, never()).save(any());
    }

    private RuleEvaluationResult eligibleResult() {
        return RuleEvaluationResult.from(List.of(
                new RuleResult(ApplicationRuleCode.PRODUCT_ACTIVE, true, "Product is active"),
                new RuleResult(ApplicationRuleCode.KYC_VERIFIED, true, "KYC is verified")
        ));
    }

    private org.mockito.ArgumentMatcher<com.digitalbank.accountopening.application.ApplicationStatusHistory>
    argThatHistory(
            ApplicationStatus from,
            ApplicationStatus to,
            String changedBy,
            String reason
    ) {
        return history -> history.getFromStatus() == from
                && history.getToStatus() == to
                && changedBy.equals(history.getChangedBy())
                && reason.equals(history.getReason());
    }

    private ApprovalCase assignedCase(ApplicationStatus applicationStatus) {
        ApprovalCase approvalCase = approvalCase(ApprovalCaseStatus.ASSIGNED, applicationStatus);
        approvalCase.setAssignedTo("STAFF001");
        approvalCase.setAssignedAt(OffsetDateTime.ofInstant(TEST_INSTANT.minusSeconds(60), ZoneOffset.UTC));
        return approvalCase;
    }

    private ApprovalCase approvalCase(ApprovalCaseStatus status, ApplicationStatus applicationStatus) {
        ApprovalCase approvalCase = new ApprovalCase();
        approvalCase.setCaseId(UUID.randomUUID());
        approvalCase.setApplication(application(applicationStatus));
        approvalCase.setStatus(status);
        approvalCase.setCreatedAt(OffsetDateTime.ofInstant(TEST_INSTANT.minusSeconds(120), ZoneOffset.UTC));
        approvalCase.setUpdatedAt(approvalCase.getCreatedAt());
        return approvalCase;
    }

    private AccountApplication application(ApplicationStatus status) {
        AccountApplication application = new AccountApplication();
        application.setApplicationId(UUID.randomUUID());
        application.setCustomerId("CUSTOMER001");
        application.setProductCode("CURRENT_ACCOUNT");
        application.setStatus(status);
        application.setReviewRequired(true);
        application.setReviewReason(ReviewReason.CUSTOMER_PROFILE_REVIEW);
        return application;
    }
}
