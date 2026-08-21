package com.digitalbank.accountopening.application.rule;

import com.digitalbank.accountopening.application.AccountApplication;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KycVerifiedRuleTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 21);
    private final KycVerifiedRule rule = new KycVerifiedRule(
            Clock.fixed(Instant.parse("2026-08-21T05:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void verifiedStatusWithFutureExpiry_shouldPass() {
        AccountApplication application = application("VERIFIED", timestamp(), TODAY.plusDays(1));

        RuleResult result = rule.evaluate(application);

        assertEquals(ApplicationRuleCode.KYC_VERIFIED, result.ruleCode());
        assertTrue(result.passed());
    }

    @Test
    void verifiedStatusWithExpiryToday_shouldPass() {
        assertTrue(rule.evaluate(application("VERIFIED", timestamp(), TODAY)).passed());
    }

    @Test
    void expiredKyc_shouldFail() {
        AccountApplication application = application("VERIFIED", timestamp(), TODAY.minusDays(1));

        RuleResult result = rule.evaluate(application);

        assertFalse(result.passed());
        assertEquals("KYC verification has expired", result.message());
    }

    @Test
    void missingExpiry_shouldFail() {
        AccountApplication application = application("VERIFIED", timestamp(), null);

        RuleResult result = rule.evaluate(application);

        assertFalse(result.passed());
        assertEquals("KYC verification expiry date is missing", result.message());
    }

    @Test
    void missingTimestamp_shouldFail() {
        AccountApplication application = application("VERIFIED", null, TODAY.plusDays(1));

        RuleResult result = rule.evaluate(application);

        assertFalse(result.passed());
        assertEquals("KYC verification timestamp is missing", result.message());
    }

    @Test
    void missingOrNonVerifiedStatus_shouldFail() {
        assertFalse(rule.evaluate(application(null, timestamp(), TODAY.plusDays(1))).passed());
        assertFalse(rule.evaluate(application("PENDING", timestamp(), TODAY.plusDays(1))).passed());
    }

    private OffsetDateTime timestamp() {
        return OffsetDateTime.parse("2026-08-20T12:00:00+07:00");
    }

    private AccountApplication application(
            String kycStatus,
            OffsetDateTime cifVerifiedAt,
            LocalDate kycExpiryDate
    ) {
        AccountApplication application = new AccountApplication();
        application.setKycStatus(kycStatus);
        application.setCifVerifiedAt(cifVerifiedAt);
        application.setKycExpiryDate(kycExpiryDate);
        return application;
    }
}
