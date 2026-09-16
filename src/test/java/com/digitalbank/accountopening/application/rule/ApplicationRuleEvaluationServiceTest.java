package com.digitalbank.accountopening.application.rule;

import com.digitalbank.accountopening.application.AccountApplication;
import com.digitalbank.accountopening.application.AccountApplicationRepository;
import com.digitalbank.accountopening.bankaccount.BankAccountRepository;
import com.digitalbank.accountopening.product.Product;
import com.digitalbank.accountopening.product.ProductRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApplicationRuleEvaluationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-21T05:00:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void activeProductAndVerifiedKyc_shouldBeEligible() {
        ApplicationRuleEvaluationService service = service();

        RuleEvaluationResult result = service.evaluate(application(
                "VERIFIED",
                OffsetDateTime.now(CLOCK),
                LocalDate.of(2026, 8, 22)
        ));

        assertTrue(result.eligible());
        assertEquals(6, result.ruleResults().size());
        assertEquals(List.of(
                        ApplicationRuleCode.REQUIRED_CUSTOMER_DATA,
                        ApplicationRuleCode.CUSTOMER_ACTIVE,
                        ApplicationRuleCode.DUPLICATE_APPLICATION,
                        ApplicationRuleCode.KYC_VERIFIED,
                        ApplicationRuleCode.PRODUCT_ACTIVE,
                        ApplicationRuleCode.EXISTING_PRODUCT_ACCOUNT
                ),
                result.ruleResults().stream().map(RuleResult::ruleCode).toList());
        assertTrue(result.failedRules().isEmpty());
    }

    @Test
    void activeProductAndUnverifiedKyc_shouldNotBeEligible() {
        ApplicationRuleEvaluationService service = service();

        RuleEvaluationResult result = service.evaluate(application(null, null, null));

        assertFalse(result.eligible());
        assertEquals(1, result.failedRules().size());
        assertEquals(ApplicationRuleCode.KYC_VERIFIED, result.failedRules().getFirst().ruleCode());
    }

    @Test
    void inactiveCustomer_shouldNotBeEligibleAndReportCustomerRule() {
        AccountApplication application = application("VERIFIED", OffsetDateTime.now(CLOCK), LocalDate.of(2026, 8, 22));
        application.setCustomerStatus("INACTIVE");

        RuleEvaluationResult result = service().evaluate(application);

        assertFalse(result.eligible());
        assertEquals(List.of(ApplicationRuleCode.CUSTOMER_ACTIVE),
                result.failedRules().stream().map(RuleResult::ruleCode).toList());
    }

    @Test
    void missingRequiredCustomerData_shouldNotBeEligibleAndReportRequiredDataRule() {
        AccountApplication application = application("VERIFIED", OffsetDateTime.now(CLOCK), LocalDate.of(2026, 8, 22));
        application.setCustomerFullName(null);

        RuleEvaluationResult result = service().evaluate(application);

        assertFalse(result.eligible());
        assertEquals(List.of(ApplicationRuleCode.REQUIRED_CUSTOMER_DATA),
                result.failedRules().stream().map(RuleResult::ruleCode).toList());
    }

    @Test
    void multipleRuleFailures_shouldReturnEveryFailedRule() {
        AccountApplication application = application(null, null, null);
        application.setCustomerFullName(" ");
        application.setCustomerStatus("BLOCKED");

        RuleEvaluationResult result = service().evaluate(application);

        assertFalse(result.eligible());
        assertEquals(List.of(
                        ApplicationRuleCode.REQUIRED_CUSTOMER_DATA,
                        ApplicationRuleCode.CUSTOMER_ACTIVE,
                        ApplicationRuleCode.KYC_VERIFIED
                ),
                result.failedRules().stream().map(RuleResult::ruleCode).toList());
    }

    private ApplicationRuleEvaluationService service() {
        ProductRepository productRepository = activeProductRepository();
        return new ApplicationRuleEvaluationService(List.of(
                new RequiredCustomerDataRule(),
                new CustomerActiveRule(),
                new DuplicateApplicationRule(mock(AccountApplicationRepository.class)),
                new KycVerifiedRule(CLOCK),
                new ProductActiveRule(productRepository),
                new ExistingProductAccountRule(productRepository, mock(BankAccountRepository.class))
        ));
    }

    private ProductRepository activeProductRepository() {
        ProductRepository productRepository = mock(ProductRepository.class);
        Product product = new Product();
        product.setActive(true);
        product.setAllowMultipleAccounts(true);
        when(productRepository.findByProductCode("DIGITAL_SAVING"))
                .thenReturn(Optional.of(product));
        return productRepository;
    }

    private AccountApplication application(
            String kycStatus,
            OffsetDateTime cifVerifiedAt,
            LocalDate kycExpiryDate
    ) {
        AccountApplication application = new AccountApplication();
        application.setProductCode("DIGITAL_SAVING");
        application.setCustomerId("CUS001");
        application.setCustomerFullName("Nguyen Van A");
        application.setCustomerDateOfBirth(LocalDate.of(1998, 5, 15));
        application.setCustomerStatus("ACTIVE");
        application.setKycStatus(kycStatus);
        application.setCifVerifiedAt(cifVerifiedAt);
        application.setKycExpiryDate(kycExpiryDate);
        return application;
    }
}
