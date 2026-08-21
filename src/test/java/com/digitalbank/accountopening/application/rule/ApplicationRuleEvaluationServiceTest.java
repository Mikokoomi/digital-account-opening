package com.digitalbank.accountopening.application.rule;

import com.digitalbank.accountopening.application.AccountApplication;
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
        ProductRepository productRepository = activeProductRepository();
        ApplicationRuleEvaluationService service = new ApplicationRuleEvaluationService(List.of(
                new ProductActiveRule(productRepository),
                new KycVerifiedRule(CLOCK)
        ));

        RuleEvaluationResult result = service.evaluate(application(
                "VERIFIED",
                OffsetDateTime.now(CLOCK),
                LocalDate.of(2026, 8, 22)
        ));

        assertTrue(result.eligible());
        assertEquals(2, result.ruleResults().size());
        assertTrue(result.failedRules().isEmpty());
    }

    @Test
    void activeProductAndUnverifiedKyc_shouldNotBeEligible() {
        ProductRepository productRepository = activeProductRepository();
        ApplicationRuleEvaluationService service = new ApplicationRuleEvaluationService(List.of(
                new ProductActiveRule(productRepository),
                new KycVerifiedRule(CLOCK)
        ));

        RuleEvaluationResult result = service.evaluate(application(null, null, null));

        assertFalse(result.eligible());
        assertEquals(1, result.failedRules().size());
        assertEquals(ApplicationRuleCode.KYC_VERIFIED, result.failedRules().getFirst().ruleCode());
    }

    private ProductRepository activeProductRepository() {
        ProductRepository productRepository = mock(ProductRepository.class);
        Product product = new Product();
        product.setActive(true);
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
        application.setKycStatus(kycStatus);
        application.setCifVerifiedAt(cifVerifiedAt);
        application.setKycExpiryDate(kycExpiryDate);
        return application;
    }
}
