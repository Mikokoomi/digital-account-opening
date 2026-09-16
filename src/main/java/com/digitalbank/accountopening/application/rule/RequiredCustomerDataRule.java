package com.digitalbank.accountopening.application.rule;

import com.digitalbank.accountopening.application.AccountApplication;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(1)
public class RequiredCustomerDataRule implements ApplicationRule {

    @Override
    public RuleResult evaluate(AccountApplication application) {
        List<String> missingFields = new ArrayList<>();

        if (application == null || isBlank(application.getCustomerId())) {
            missingFields.add("customerId");
        }
        if (application == null || isBlank(application.getCustomerFullName())) {
            missingFields.add("fullName");
        }
        if (application == null || application.getCustomerDateOfBirth() == null) {
            missingFields.add("dateOfBirth");
        }

        if (!missingFields.isEmpty()) {
            return new RuleResult(
                    ApplicationRuleCode.REQUIRED_CUSTOMER_DATA,
                    false,
                    "Required customer data is missing: " + String.join(", ", missingFields)
            );
        }

        return new RuleResult(
                ApplicationRuleCode.REQUIRED_CUSTOMER_DATA,
                true,
                "Required customer data is present"
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
