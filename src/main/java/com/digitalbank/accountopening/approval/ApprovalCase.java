package com.digitalbank.accountopening.approval;

import com.digitalbank.accountopening.application.AccountApplication;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "approval_cases")
@Getter
@Setter
@NoArgsConstructor
public class ApprovalCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "case_id", nullable = false, updatable = false)
    private UUID caseId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, updatable = false)
    private AccountApplication application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApprovalCaseStatus status;

    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    @Column(name = "review_reason", length = 500)
    private String reviewReason;

    @Column(name = "decision_reason", length = 500)
    private String decisionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
