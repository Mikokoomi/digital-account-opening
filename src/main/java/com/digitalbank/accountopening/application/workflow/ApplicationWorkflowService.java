package com.digitalbank.accountopening.application.workflow;

import com.digitalbank.accountopening.application.AccountApplication;
import com.digitalbank.accountopening.application.ApplicationStatusHistory;
import com.digitalbank.accountopening.application.ApplicationStatusHistoryRepository;
import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import com.digitalbank.accountopening.common.exception.InvalidApplicationStatusTransitionException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationWorkflowService {

    private final ApplicationStatusHistoryRepository historyRepository;

    public ApplicationWorkflowService(ApplicationStatusHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Transactional
    public void transition(
            AccountApplication application,
            ApplicationStatus targetStatus,
            String changedBy,
            String reason
    ) {
        ApplicationStatus currentStatus = application.getStatus();
        if (!isAllowedTransition(currentStatus, targetStatus)) {
            throw new InvalidApplicationStatusTransitionException(currentStatus, targetStatus);
        }

        application.setStatus(targetStatus);

        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setApplication(application);
        history.setFromStatus(currentStatus);
        history.setToStatus(targetStatus);
        history.setChangedBy(changedBy);
        history.setReason(reason);
        historyRepository.save(history);
    }

    private boolean isAllowedTransition(
            ApplicationStatus currentStatus,
            ApplicationStatus targetStatus
    ) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }

        return switch (currentStatus) {
            case DRAFT -> targetStatus == ApplicationStatus.SUBMITTED
                    || targetStatus == ApplicationStatus.CANCELLED;
            case SUBMITTED -> targetStatus == ApplicationStatus.UNDER_REVIEW
                    || targetStatus == ApplicationStatus.APPROVED
                    || targetStatus == ApplicationStatus.CANCELLED;
            case UNDER_REVIEW -> targetStatus == ApplicationStatus.APPROVED
                    || targetStatus == ApplicationStatus.REJECTED;
            case APPROVED -> targetStatus == ApplicationStatus.ACCOUNT_CREATING;
            case ACCOUNT_CREATING -> targetStatus == ApplicationStatus.COMPLETED
                    || targetStatus == ApplicationStatus.RETRY_PENDING;
            case RETRY_PENDING -> targetStatus == ApplicationStatus.ACCOUNT_CREATING;
            case COMPLETED, REJECTED, CANCELLED, FAILED -> false;
        };
    }
}
