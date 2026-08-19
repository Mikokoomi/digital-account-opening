package com.digitalbank.accountopening.application.rule;
import org.springframework.core.annotation.Order;
import com.digitalbank.accountopening.application.AccountApplication;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class KycVerifiedRule implements ApplicationRule {

    private static final String VERIFIED = "VERIFIED";

    @Override
    public RuleResult evaluate(AccountApplication application) {
        if (!VERIFIED.equals(application.getKycStatus())) {
            return new RuleResult(
                    ApplicationRuleCode.KYC_VERIFIED,
                    false,
                    "KYC has not been verified"
            );
        }

        if (application.getCifVerifiedAt() == null) {
            return new RuleResult(
                    ApplicationRuleCode.KYC_VERIFIED,
                    false,
                    "KYC verification timestamp is missing"
            );
        }

        return new RuleResult(
                ApplicationRuleCode.KYC_VERIFIED,
                true,
                "KYC verification is confirmed"
        );
    }
}
