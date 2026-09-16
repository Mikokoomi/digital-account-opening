package com.digitalbank.accountopening.application.rule;

import com.digitalbank.accountopening.application.AccountApplication;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@Order(2)
public class CustomerActiveRule implements ApplicationRule {

    private static final String ACTIVE = "ACTIVE";

    @Override
    public RuleResult evaluate(AccountApplication application) {
        if (application == null) {
            return failed("Customer data is unavailable");
        }

        String customerStatus = application.getCustomerStatus();
        if (customerStatus == null || customerStatus.isBlank()) {
            return failed("Customer status is missing");
        }

        String normalizedStatus = customerStatus.trim().toUpperCase(Locale.ROOT);
        if (!ACTIVE.equals(normalizedStatus)) {
            return failed("Customer is not active: " + customerStatus);
        }

        return new RuleResult(
                ApplicationRuleCode.CUSTOMER_ACTIVE,
                true,
                "Customer is active"
        );
    }

    private RuleResult failed(String message) {
        return new RuleResult(ApplicationRuleCode.CUSTOMER_ACTIVE, false, message);
    }
}
