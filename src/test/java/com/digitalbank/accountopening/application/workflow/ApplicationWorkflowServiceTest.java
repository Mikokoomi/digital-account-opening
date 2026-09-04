package com.digitalbank.accountopening.application.workflow;

import com.digitalbank.accountopening.application.AccountApplication;
import com.digitalbank.accountopening.application.ApplicationStatusHistory;
import com.digitalbank.accountopening.application.ApplicationStatusHistoryRepository;
import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import com.digitalbank.accountopening.common.exception.InvalidApplicationStatusTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApplicationWorkflowServiceTest {

    @Mock
    private ApplicationStatusHistoryRepository historyRepository;

    @Test
    void transition_shouldApplyEveryAllowedTransitionAndPersistOneHistoryEntry() {
        ApplicationWorkflowService workflowService = new ApplicationWorkflowService(historyRepository);
        List<TransitionCase> transitions = List.of(
                new TransitionCase(ApplicationStatus.DRAFT, ApplicationStatus.SUBMITTED),
                new TransitionCase(ApplicationStatus.DRAFT, ApplicationStatus.CANCELLED),
                new TransitionCase(ApplicationStatus.SUBMITTED, ApplicationStatus.UNDER_REVIEW),
                new TransitionCase(ApplicationStatus.SUBMITTED, ApplicationStatus.APPROVED),
                new TransitionCase(ApplicationStatus.SUBMITTED, ApplicationStatus.CANCELLED),
                new TransitionCase(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.APPROVED),
                new TransitionCase(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.REJECTED),
                new TransitionCase(ApplicationStatus.APPROVED, ApplicationStatus.ACCOUNT_CREATING),
                new TransitionCase(ApplicationStatus.ACCOUNT_CREATING, ApplicationStatus.COMPLETED)
        );

        for (TransitionCase transition : transitions) {
            AccountApplication application = application(transition.fromStatus());
            workflowService.transition(application, transition.toStatus(), "SYSTEM", "Test transition");
            assertEquals(transition.toStatus(), application.getStatus());
        }

        ArgumentCaptor<ApplicationStatusHistory> historyCaptor =
                ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(historyRepository, org.mockito.Mockito.times(transitions.size())).save(historyCaptor.capture());

        for (int index = 0; index < transitions.size(); index++) {
            TransitionCase transition = transitions.get(index);
            ApplicationStatusHistory history = historyCaptor.getAllValues().get(index);
            assertEquals(transition.fromStatus(), history.getFromStatus());
            assertEquals(transition.toStatus(), history.getToStatus());
            assertEquals("SYSTEM", history.getChangedBy());
            assertEquals("Test transition", history.getReason());
        }
    }

    @Test
    void transition_shouldRejectInvalidAndTerminalTransitionsWithoutHistory() {
        ApplicationWorkflowService workflowService = new ApplicationWorkflowService(historyRepository);
        List<TransitionCase> transitions = List.of(
                new TransitionCase(ApplicationStatus.DRAFT, ApplicationStatus.APPROVED),
                new TransitionCase(ApplicationStatus.SUBMITTED, ApplicationStatus.DRAFT),
                new TransitionCase(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.CANCELLED),
                new TransitionCase(ApplicationStatus.APPROVED, ApplicationStatus.SUBMITTED),
                new TransitionCase(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED),
                new TransitionCase(ApplicationStatus.ACCOUNT_CREATING, ApplicationStatus.APPROVED),
                new TransitionCase(ApplicationStatus.COMPLETED, ApplicationStatus.ACCOUNT_CREATING),
                new TransitionCase(ApplicationStatus.REJECTED, ApplicationStatus.UNDER_REVIEW),
                new TransitionCase(ApplicationStatus.CANCELLED, ApplicationStatus.APPROVED),
                new TransitionCase(ApplicationStatus.FAILED, ApplicationStatus.SUBMITTED)
        );

        for (TransitionCase transition : transitions) {
            AccountApplication application = application(transition.fromStatus());
            InvalidApplicationStatusTransitionException exception = assertThrows(
                    InvalidApplicationStatusTransitionException.class,
                    () -> workflowService.transition(
                            application,
                            transition.toStatus(),
                            "SYSTEM",
                            "Test transition"
                    )
            );
            assertEquals(
                    "Application status transition from "
                            + transition.fromStatus() + " to " + transition.toStatus()
                            + " is not allowed.",
                    exception.getMessage()
            );
            assertEquals(transition.fromStatus(), application.getStatus());
        }

        verify(historyRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void transition_shouldRejectNullCurrentStatusWithoutHistory() {
        ApplicationWorkflowService workflowService = new ApplicationWorkflowService(historyRepository);
        AccountApplication application = application(null);

        assertThrows(
                InvalidApplicationStatusTransitionException.class,
                () -> workflowService.transition(
                        application,
                        ApplicationStatus.SUBMITTED,
                        "SYSTEM",
                        "Test transition"
                )
        );

        verify(historyRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private AccountApplication application(ApplicationStatus status) {
        AccountApplication application = new AccountApplication();
        application.setStatus(status);
        return application;
    }

    private record TransitionCase(ApplicationStatus fromStatus, ApplicationStatus toStatus) {
    }
}
