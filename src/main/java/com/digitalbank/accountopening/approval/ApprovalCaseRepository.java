package com.digitalbank.accountopening.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalCaseRepository extends JpaRepository<ApprovalCase, UUID> {

    boolean existsByApplicationApplicationId(UUID applicationId);

    Optional<ApprovalCase> findByApplicationApplicationId(UUID applicationId);

    List<ApprovalCase> findAllByOrderByCreatedAtAsc();

    List<ApprovalCase> findAllByStatusOrderByCreatedAtAsc(ApprovalCaseStatus status);

    List<ApprovalCase> findAllByAssignedToOrderByCreatedAtAsc(String assignedTo);

    List<ApprovalCase> findAllByStatusAndAssignedToOrderByCreatedAtAsc(
            ApprovalCaseStatus status,
            String assignedTo
    );
}
