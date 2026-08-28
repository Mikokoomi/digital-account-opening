package com.digitalbank.accountopening.approval;

import com.digitalbank.accountopening.application.AccountApplication;
import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import com.digitalbank.accountopening.application.rule.ApplicationRuleEvaluationService;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ApprovalCaseService {

    private final ApprovalCaseRepository approvalCaseRepository;
    private final ApplicationWorkflowService applicationWorkflowService;
    private final ApplicationRuleEvaluationService applicationRuleEvaluationService;
    private final Clock clock;

    public ApprovalCaseService(
            ApprovalCaseRepository approvalCaseRepository,
            ApplicationWorkflowService applicationWorkflowService,
            ApplicationRuleEvaluationService applicationRuleEvaluationService,
            Clock clock
    ) {
        this.approvalCaseRepository = approvalCaseRepository;
        this.applicationWorkflowService = applicationWorkflowService;
        this.applicationRuleEvaluationService = applicationRuleEvaluationService;
        this.clock = clock;
    }

    @Transactional
    public ApprovalCase createForManualReview(AccountApplication application, String reviewReason) {
        if (application.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new ApplicationNotUnderReviewException(application.getStatus());
        }
        if (!Boolean.TRUE.equals(application.getReviewRequired())
                || application.getReviewReason() == null) {
            throw new ManualReviewDataInvalidException();
        }
        UUID applicationId = application.getApplicationId();
        if (approvalCaseRepository.existsByApplicationApplicationId(applicationId)) {
            throw new ApprovalCaseAlreadyExistsException(applicationId);
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        ApprovalCase approvalCase = new ApprovalCase();
        approvalCase.setApplication(application);
        approvalCase.setStatus(ApprovalCaseStatus.PENDING);
        approvalCase.setReviewReason(reviewReason);
        approvalCase.setCreatedAt(now);
        approvalCase.setUpdatedAt(now);

        try {
            return approvalCaseRepository.saveAndFlush(approvalCase);
        } catch (DataIntegrityViolationException exception) {
            throw new ApprovalCaseAlreadyExistsException(applicationId);
        }
    }

    @Transactional(readOnly = true)
    public List<ApprovalCaseResponse> getApprovalCases(
            ApprovalCaseStatus status,
            String assignedTo
    ) {
        String normalizedAssignedTo = normalizeOptional(assignedTo);
        List<ApprovalCase> cases;
        if (status != null && normalizedAssignedTo != null) {
            cases = approvalCaseRepository.findAllByStatusAndAssignedToOrderByCreatedAtAsc(
                    status,
                    normalizedAssignedTo
            );
        } else if (status != null) {
            cases = approvalCaseRepository.findAllByStatusOrderByCreatedAtAsc(status);
        } else if (normalizedAssignedTo != null) {
            cases = approvalCaseRepository.findAllByAssignedToOrderByCreatedAtAsc(normalizedAssignedTo);
        } else {
            cases = approvalCaseRepository.findAllByOrderByCreatedAtAsc();
        }
        return cases.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ApprovalCaseResponse getApprovalCase(UUID caseId) {
        return toResponse(findCase(caseId));
    }

    @Transactional
    public ApprovalCaseResponse assign(UUID caseId, String staffId) {
        ApprovalCase approvalCase = findCase(caseId);
        if (approvalCase.getStatus() != ApprovalCaseStatus.PENDING) {
            throw new ApprovalCaseAssignmentNotAllowedException(approvalCase.getStatus());
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        approvalCase.setAssignedTo(staffId.trim());
        approvalCase.setAssignedAt(now);
        approvalCase.setUpdatedAt(now);
        approvalCase.setStatus(ApprovalCaseStatus.ASSIGNED);
        return toResponse(approvalCase);
    }

    @Transactional
    public ApprovalCaseResponse approve(UUID caseId, String staffId) {
        ApprovalCase approvalCase = validateDecision(caseId, staffId);
        if (!applicationRuleEvaluationService.evaluate(approvalCase.getApplication()).eligible()) {
            throw new ApprovalCaseBusinessRulesNotSatisfiedException();
        }
        String normalizedStaffId = staffId.trim();
        applicationWorkflowService.transition(
                approvalCase.getApplication(),
                ApplicationStatus.APPROVED,
                normalizedStaffId,
                "Application approved by staff"
        );
        completeDecision(approvalCase, ApprovalCaseStatus.APPROVED, null);
        return toResponse(approvalCase);
    }

    @Transactional
    public ApprovalCaseResponse reject(UUID caseId, String staffId, String reason) {
        ApprovalCase approvalCase = validateDecision(caseId, staffId);
        String normalizedReason = reason.trim();
        applicationWorkflowService.transition(
                approvalCase.getApplication(),
                ApplicationStatus.REJECTED,
                staffId.trim(),
                normalizedReason
        );
        approvalCase.getApplication().setRejectReason(normalizedReason);
        completeDecision(approvalCase, ApprovalCaseStatus.REJECTED, normalizedReason);
        return toResponse(approvalCase);
    }

    private ApprovalCase validateDecision(UUID caseId, String staffId) {
        ApprovalCase approvalCase = findCase(caseId);
        if (approvalCase.getStatus() != ApprovalCaseStatus.ASSIGNED) {
            throw new ApprovalCaseDecisionNotAllowedException(approvalCase.getStatus());
        }
        if (!staffId.trim().equals(approvalCase.getAssignedTo())) {
            throw new ApprovalCaseStaffMismatchException();
        }
        if (approvalCase.getApplication().getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new ApplicationNotUnderReviewException(approvalCase.getApplication().getStatus());
        }
        return approvalCase;
    }

    private void completeDecision(
            ApprovalCase approvalCase,
            ApprovalCaseStatus status,
            String decisionReason
    ) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        approvalCase.setStatus(status);
        approvalCase.setDecisionReason(decisionReason);
        approvalCase.setDecidedAt(now);
        approvalCase.setUpdatedAt(now);
    }

    private ApprovalCase findCase(UUID caseId) {
        return approvalCaseRepository.findById(caseId)
                .orElseThrow(() -> new ApprovalCaseNotFoundException(caseId));
    }

    private ApprovalCaseResponse toResponse(ApprovalCase approvalCase) {
        AccountApplication application = approvalCase.getApplication();
        return new ApprovalCaseResponse(
                approvalCase.getCaseId(),
                application.getApplicationId(),
                application.getStatus(),
                application.getCustomerId(),
                application.getProductCode(),
                approvalCase.getStatus(),
                approvalCase.getAssignedTo(),
                approvalCase.getReviewReason(),
                approvalCase.getDecisionReason(),
                approvalCase.getCreatedAt(),
                approvalCase.getAssignedAt(),
                approvalCase.getDecidedAt(),
                approvalCase.getUpdatedAt()
        );
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
