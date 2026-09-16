package com.digitalbank.accountopening.application.rule;

import com.digitalbank.accountopening.application.AccountApplication;
import com.digitalbank.accountopening.application.AccountApplicationRepository;
import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
@Order(3)
public class DuplicateApplicationRule implements ApplicationRule {

    private static final Set<ApplicationStatus> PROCESSING_STATUSES = EnumSet.of(
            ApplicationStatus.SUBMITTED,
            ApplicationStatus.UNDER_REVIEW,
            ApplicationStatus.APPROVED,
            ApplicationStatus.ACCOUNT_CREATING,
            ApplicationStatus.RETRY_PENDING
    );

    private final AccountApplicationRepository applicationRepository;

    public DuplicateApplicationRule(AccountApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Override
    public RuleResult evaluate(AccountApplication application) {
        boolean duplicate = applicationRepository
                .existsByCustomerIdAndProductCodeAndApplicationIdNotAndStatusIn(
                        application.getCustomerId(),
                        application.getProductCode(),
                        application.getApplicationId(),
                        PROCESSING_STATUSES
                );

        return new RuleResult(
                ApplicationRuleCode.DUPLICATE_APPLICATION,
                !duplicate,
                duplicate
                        ? "Another application for this customer and product is already being processed"
                        : "No duplicate application is being processed"
        );
    }
}
