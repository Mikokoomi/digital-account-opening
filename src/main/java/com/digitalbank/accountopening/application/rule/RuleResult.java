package com.digitalbank.accountopening.application.rule;

import java.util.Objects;

public record RuleResult(
        ApplicationRuleCode ruleCode,
        boolean passed,
        String message
) {

    public RuleResult {
        Objects.requireNonNull(ruleCode, "ruleCode must not be null");
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
