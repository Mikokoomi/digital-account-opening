package com.digitalbank.accountopening.application.rule;

import com.digitalbank.accountopening.application.AccountApplication;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerActiveRuleTest {

    private final CustomerActiveRule rule = new CustomerActiveRule();

    @Test
    void activeCustomer_shouldPass() {
        assertTrue(rule.evaluate(application("ACTIVE")).passed());
        assertTrue(rule.evaluate(application(" active ")).passed());
    }

    @Test
    void blockedCustomer_shouldFail() {
        assertFalse(rule.evaluate(application("BLOCKED")).passed());
    }

    @Test
    void inactiveCustomer_shouldFail() {
        assertFalse(rule.evaluate(application("INACTIVE")).passed());
    }

    @Test
    void missingCustomer_shouldFailSafely() {
        RuleResult result = assertDoesNotThrow(() -> rule.evaluate(null));
        assertFalse(result.passed());
    }

    @Test
    void nullOrBlankStatus_shouldFailSafely() {
        assertFalse(assertDoesNotThrow(() -> rule.evaluate(application(null))).passed());
        assertFalse(assertDoesNotThrow(() -> rule.evaluate(application("  "))).passed());
        assertFalse(assertDoesNotThrow(() -> rule.evaluate(application("UNKNOWN"))).passed());
    }

    private AccountApplication application(String customerStatus) {
        AccountApplication application = new AccountApplication();
        application.setCustomerStatus(customerStatus);
        return application;
    }
}
