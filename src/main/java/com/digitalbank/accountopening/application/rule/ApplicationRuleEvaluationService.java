package com.digitalbank.accountopening.application.rule;

import com.digitalbank.accountopening.application.AccountApplication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ApplicationRuleEvaluationService {

    private final List<ApplicationRule> rules;

    public ApplicationRuleEvaluationService(List<ApplicationRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public RuleEvaluationResult evaluate(AccountApplication application) {
        Objects.requireNonNull(application, "application must not be null");

        List<RuleResult> ruleResults = rules.stream()
                .map(rule -> Objects.requireNonNull(
                        rule.evaluate(application),
                        "Application rule must return a result"
                ))
                .toList();

        return RuleEvaluationResult.from(ruleResults);
    }
}
