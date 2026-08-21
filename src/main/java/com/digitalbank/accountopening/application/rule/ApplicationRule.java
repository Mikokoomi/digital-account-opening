package com.digitalbank.accountopening.application.rule;

import com.digitalbank.accountopening.application.AccountApplication;

public interface ApplicationRule {

    RuleResult evaluate(AccountApplication application);
}
