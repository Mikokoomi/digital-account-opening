package com.digitalbank.accountopening.application.rule;

import com.digitalbank.accountopening.application.AccountApplication;
import com.digitalbank.accountopening.bankaccount.BankAccountRepository;
import com.digitalbank.accountopening.product.Product;
import com.digitalbank.accountopening.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExistingProductAccountRuleTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @InjectMocks
    private ExistingProductAccountRule rule;

    @Test
    void productAllowsMultipleAccounts_shouldPassWithoutOwnershipQuery() {
        AccountApplication application = application();
        when(productRepository.findByProductCode(application.getProductCode()))
                .thenReturn(Optional.of(product(true)));

        RuleResult result = rule.evaluate(application);

        assertTrue(result.passed());
        assertEquals(ApplicationRuleCode.EXISTING_PRODUCT_ACCOUNT, result.ruleCode());
        verify(bankAccountRepository, never())
                .existsByApplicationCustomerIdAndApplicationProductCodeAndApplicationApplicationIdNot(
                        application.getCustomerId(), application.getProductCode(), application.getApplicationId());
    }

    @Test
    void productDisallowsMultipleAccountsAndCustomerAlreadyOwnsOne_shouldFail() {
        AccountApplication application = application();
        when(productRepository.findByProductCode(application.getProductCode()))
                .thenReturn(Optional.of(product(false)));
        when(bankAccountRepository
                .existsByApplicationCustomerIdAndApplicationProductCodeAndApplicationApplicationIdNot(
                        application.getCustomerId(), application.getProductCode(), application.getApplicationId()))
                .thenReturn(true);

        RuleResult result = rule.evaluate(application);

        assertFalse(result.passed());
        assertEquals(ApplicationRuleCode.EXISTING_PRODUCT_ACCOUNT, result.ruleCode());
    }

    @Test
    void productDisallowsMultipleAccountsAndCustomerDoesNotOwnOne_shouldPass() {
        AccountApplication application = application();
        when(productRepository.findByProductCode(application.getProductCode()))
                .thenReturn(Optional.of(product(false)));
        when(bankAccountRepository
                .existsByApplicationCustomerIdAndApplicationProductCodeAndApplicationApplicationIdNot(
                        application.getCustomerId(), application.getProductCode(), application.getApplicationId()))
                .thenReturn(false);

        assertTrue(rule.evaluate(application).passed());
    }

    private AccountApplication application() {
        AccountApplication application = new AccountApplication();
        application.setApplicationId(UUID.randomUUID());
        application.setCustomerId("CUS001");
        application.setProductCode("CURRENT_ACCOUNT");
        return application;
    }

    private Product product(boolean allowMultipleAccounts) {
        Product product = new Product();
        product.setAllowMultipleAccounts(allowMultipleAccounts);
        return product;
    }
}
