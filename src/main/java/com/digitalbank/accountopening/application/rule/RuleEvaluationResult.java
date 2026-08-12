package com.digitalbank.accountopening.application.rule;

import java.util.List;
import java.util.Objects;

public record RuleEvaluationResult(
        boolean eligible,
        List<RuleResult> ruleResults,
        List<RuleResult> failedRules
) {

    public static RuleEvaluationResult from(List<RuleResult> ruleResults) {
        List<RuleResult> copiedRuleResults = List.copyOf(
                Objects.requireNonNull(ruleResults, "ruleResults must not be null")
        );
        List<RuleResult> failedRules = copiedRuleResults.stream()
                .filter(ruleResult -> !ruleResult.passed())
                .toList();

        boolean eligible = !copiedRuleResults.isEmpty() && failedRules.isEmpty();
        return new RuleEvaluationResult(eligible, copiedRuleResults, failedRules);
    }
}
