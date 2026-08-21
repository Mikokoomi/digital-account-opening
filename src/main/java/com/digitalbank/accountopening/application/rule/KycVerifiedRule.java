package com.digitalbank.accountopening.application.rule;

import com.digitalbank.accountopening.application.AccountApplication;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
@Order(2)
public class KycVerifiedRule implements ApplicationRule {

    private static final String VERIFIED = "VERIFIED";

    private final Clock clock;

    public KycVerifiedRule(Clock clock) {
        this.clock = clock;
    }

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

        LocalDate kycExpiryDate = application.getKycExpiryDate();
        if (kycExpiryDate == null) {
            return new RuleResult(
                    ApplicationRuleCode.KYC_VERIFIED,
                    false,
                    "KYC verification expiry date is missing"
            );
        }

        if (kycExpiryDate.isBefore(LocalDate.now(clock))) {
            return new RuleResult(
                    ApplicationRuleCode.KYC_VERIFIED,
                    false,
                    "KYC verification has expired"
            );
        }

        return new RuleResult(
                ApplicationRuleCode.KYC_VERIFIED,
                true,
                "KYC verification is confirmed"
        );
    }
}
