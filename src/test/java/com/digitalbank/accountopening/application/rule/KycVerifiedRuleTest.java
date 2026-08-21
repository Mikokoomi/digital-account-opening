package com.digitalbank.accountopening.application.rule;

import com.digitalbank.accountopening.application.AccountApplication;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KycVerifiedRuleTest {

    private final KycVerifiedRule rule = new KycVerifiedRule();

    @Test
    void verifiedStatusAndTimestamp_shouldPass() {
        AccountApplication application = application("VERIFIED", OffsetDateTime.parse("2026-08-12T12:00:00+07:00"));

        RuleResult result = rule.evaluate(application);

        assertEquals(ApplicationRuleCode.KYC_VERIFIED, result.ruleCode());
        assertTrue(result.passed());
    }

    @Test
    void nonVerifiedStatus_shouldFail() {
        AccountApplication application = application("PENDING", OffsetDateTime.parse("2026-08-12T12:00:00+07:00"));

        RuleResult result = rule.evaluate(application);

        assertFalse(result.passed());
        assertEquals("KYC has not been verified", result.message());
    }

    @Test
    void verifiedStatusWithoutTimestamp_shouldFail() {
        AccountApplication application = application("VERIFIED", null);

        RuleResult result = rule.evaluate(application);

        assertFalse(result.passed());
        assertEquals("KYC verification timestamp is missing", result.message());
    }

    @Test
    void nullStatus_shouldFail() {
        AccountApplication application = application(null, OffsetDateTime.parse("2026-08-12T12:00:00+07:00"));

        RuleResult result = rule.evaluate(application);

        assertFalse(result.passed());
        assertEquals("KYC has not been verified", result.message());
    }

    private AccountApplication application(String kycStatus, OffsetDateTime cifVerifiedAt) {
        AccountApplication application = new AccountApplication();
        application.setKycStatus(kycStatus);
        application.setCifVerifiedAt(cifVerifiedAt);
        return application;
    }
}
