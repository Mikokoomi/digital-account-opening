package com.digitalbank.accountopening.application.rule;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleEvaluationResultTest {

    @Test
    void allRulesPass_shouldBeEligible() {
        RuleEvaluationResult result = RuleEvaluationResult.from(List.of(
                result(ApplicationRuleCode.PRODUCT_ACTIVE, true),
                result(ApplicationRuleCode.KYC_VERIFIED, true)
        ));

        assertTrue(result.eligible());
        assertEquals(2, result.ruleResults().size());
        assertTrue(result.failedRules().isEmpty());
    }

    @Test
    void oneRuleFails_shouldNotBeEligible() {
        RuleEvaluationResult result = RuleEvaluationResult.from(List.of(
                result(ApplicationRuleCode.PRODUCT_ACTIVE, true),
                result(ApplicationRuleCode.KYC_VERIFIED, false)
        ));

        assertFalse(result.eligible());
        assertEquals(1, result.failedRules().size());
        assertEquals(ApplicationRuleCode.KYC_VERIFIED, result.failedRules().getFirst().ruleCode());
    }

    @Test
    void multipleRulesFail_shouldReturnAllFailedRules() {
        RuleEvaluationResult result = RuleEvaluationResult.from(List.of(
                result(ApplicationRuleCode.PRODUCT_ACTIVE, false),
                result(ApplicationRuleCode.KYC_VERIFIED, true),
                result(ApplicationRuleCode.DUPLICATE_PRODUCT, false)
        ));

        assertFalse(result.eligible());
        assertEquals(
                List.of(ApplicationRuleCode.PRODUCT_ACTIVE, ApplicationRuleCode.DUPLICATE_PRODUCT),
                result.failedRules().stream().map(RuleResult::ruleCode).toList()
        );
    }

    private RuleResult result(ApplicationRuleCode ruleCode, boolean passed) {
        return new RuleResult(ruleCode, passed, "Rule result for " + ruleCode);
    }
}
