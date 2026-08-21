package com.digitalbank.accountopening.application.rule;

import com.digitalbank.accountopening.application.AccountApplication;
import com.digitalbank.accountopening.common.exception.ProductNotFoundException;
import com.digitalbank.accountopening.product.Product;
import com.digitalbank.accountopening.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductActiveRuleTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductActiveRule rule;

    @Test
    void activeProduct_shouldPass() {
        AccountApplication application = application("DIGITAL_SAVING");
        when(productRepository.findByProductCode("DIGITAL_SAVING"))
                .thenReturn(Optional.of(product(true)));

        RuleResult result = rule.evaluate(application);

        assertEquals(ApplicationRuleCode.PRODUCT_ACTIVE, result.ruleCode());
        assertTrue(result.passed());
        assertEquals("Product is active", result.message());
    }

    @Test
    void inactiveProduct_shouldFail() {
        AccountApplication application = application("DIGITAL_SAVING");
        when(productRepository.findByProductCode("DIGITAL_SAVING"))
                .thenReturn(Optional.of(product(false)));

        RuleResult result = rule.evaluate(application);

        assertEquals(ApplicationRuleCode.PRODUCT_ACTIVE, result.ruleCode());
        assertFalse(result.passed());
        assertEquals("Product is inactive", result.message());
    }

    @Test
    void missingProduct_shouldThrowProductNotFound() {
        AccountApplication application = application("UNKNOWN_PRODUCT");
        when(productRepository.findByProductCode("UNKNOWN_PRODUCT")).thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> rule.evaluate(application)
        );

        assertEquals("Product not found: UNKNOWN_PRODUCT", exception.getMessage());
    }

    private AccountApplication application(String productCode) {
        AccountApplication application = new AccountApplication();
        application.setProductCode(productCode);
        return application;
    }

    private Product product(boolean active) {
        Product product = new Product();
        product.setActive(active);
        return product;
    }
}
