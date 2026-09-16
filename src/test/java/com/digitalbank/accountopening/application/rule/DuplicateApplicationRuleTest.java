package com.digitalbank.accountopening.application.rule;

import com.digitalbank.accountopening.application.AccountApplication;
import com.digitalbank.accountopening.application.AccountApplicationRepository;
import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DuplicateApplicationRuleTest {

    @Mock
    private AccountApplicationRepository applicationRepository;

    @InjectMocks
    private DuplicateApplicationRule rule;

    @Test
    void sameCustomerAndProductInProcessingStatus_shouldFail() {
        AccountApplication application = application();
        when(applicationRepository.existsByCustomerIdAndProductCodeAndApplicationIdNotAndStatusIn(
                application.getCustomerId(), application.getProductCode(), application.getApplicationId(),
                processingStatuses()
        )).thenReturn(true);

        RuleResult result = rule.evaluate(application);

        assertFalse(result.passed());
    }

    @Test
    void noProcessingConflictIncludingTerminalDifferentCustomerOrProduct_shouldPass() {
        AccountApplication application = application();
        when(applicationRepository.existsByCustomerIdAndProductCodeAndApplicationIdNotAndStatusIn(
                application.getCustomerId(), application.getProductCode(), application.getApplicationId(),
                processingStatuses()
        )).thenReturn(false);

        RuleResult result = rule.evaluate(application);

        assertTrue(result.passed());
    }

    @Test
    void query_shouldExcludeCurrentApplicationAndUseOnlyProcessingStatuses() {
        AccountApplication application = application();

        rule.evaluate(application);

        verify(applicationRepository).existsByCustomerIdAndProductCodeAndApplicationIdNotAndStatusIn(
                application.getCustomerId(), application.getProductCode(), application.getApplicationId(),
                processingStatuses()
        );
    }

    @Test
    void multipleMatchingRecords_shouldStillReturnDeterministicFailure() {
        AccountApplication application = application();
        when(applicationRepository.existsByCustomerIdAndProductCodeAndApplicationIdNotAndStatusIn(
                application.getCustomerId(), application.getProductCode(), application.getApplicationId(),
                processingStatuses()
        )).thenReturn(true);

        assertFalse(rule.evaluate(application).passed());
        assertFalse(rule.evaluate(application).passed());
    }

    private AccountApplication application() {
        AccountApplication application = new AccountApplication();
        application.setApplicationId(UUID.randomUUID());
        application.setCustomerId("CUS001");
        application.setProductCode("CURRENT_ACCOUNT");
        return application;
    }

    private Set<ApplicationStatus> processingStatuses() {
        return EnumSet.of(
                ApplicationStatus.SUBMITTED,
                ApplicationStatus.UNDER_REVIEW,
                ApplicationStatus.APPROVED,
                ApplicationStatus.ACCOUNT_CREATING,
                ApplicationStatus.RETRY_PENDING
        );
    }
}
