package com.digitalbank.accountopening.application.rule;

import com.digitalbank.accountopening.application.AccountApplication;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequiredCustomerDataRuleTest {

    private final RequiredCustomerDataRule rule = new RequiredCustomerDataRule();

    @Test
    void completeCustomerData_shouldPass() {
        assertTrue(rule.evaluate(application("CUS001", "Nguyen Van A", LocalDate.of(1998, 5, 15))).passed());
    }

    @Test
    void nullCustomerId_shouldFail() {
        assertFalse(rule.evaluate(application(null, "Nguyen Van A", LocalDate.of(1998, 5, 15))).passed());
    }

    @Test
    void nullFullName_shouldFail() {
        assertFalse(rule.evaluate(application("CUS001", null, LocalDate.of(1998, 5, 15))).passed());
    }

    @Test
    void nullDateOfBirth_shouldFail() {
        assertFalse(rule.evaluate(application("CUS001", "Nguyen Van A", null)).passed());
    }

    @Test
    void blankStringFields_shouldFail() {
        assertFalse(rule.evaluate(application(" ", "\t", LocalDate.of(1998, 5, 15))).passed());
    }

    @Test
    void missingApplication_shouldFailWithoutUnexpectedException() {
        RuleResult result = assertDoesNotThrow(() -> rule.evaluate(null));
        assertFalse(result.passed());
    }

    private AccountApplication application(String customerId, String fullName, LocalDate dateOfBirth) {
        AccountApplication application = new AccountApplication();
        application.setCustomerId(customerId);
        application.setCustomerFullName(fullName);
        application.setCustomerDateOfBirth(dateOfBirth);
        return application;
    }
}
