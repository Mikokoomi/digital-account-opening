package com.digitalbank.accountopening.application.dto;

import com.digitalbank.accountopening.application.rule.RuleResult;

import java.util.List;
import java.util.UUID;

public record ApplicationRuleEvaluationResponse(
        UUID applicationId,
        boolean eligible,
        List<RuleResult> ruleResults,
        List<RuleResult> failedRules
) {
}