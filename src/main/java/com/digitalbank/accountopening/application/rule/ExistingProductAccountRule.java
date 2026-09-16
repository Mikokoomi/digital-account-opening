package com.digitalbank.accountopening.application.rule;

import com.digitalbank.accountopening.application.AccountApplication;
import com.digitalbank.accountopening.bankaccount.BankAccountRepository;
import com.digitalbank.accountopening.common.exception.ProductNotFoundException;
import com.digitalbank.accountopening.product.Product;
import com.digitalbank.accountopening.product.ProductRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(6)
public class ExistingProductAccountRule implements ApplicationRule {

    private final ProductRepository productRepository;
    private final BankAccountRepository bankAccountRepository;

    public ExistingProductAccountRule(
            ProductRepository productRepository,
            BankAccountRepository bankAccountRepository
    ) {
        this.productRepository = productRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    @Override
    public RuleResult evaluate(AccountApplication application) {
        Product product = productRepository.findByProductCode(application.getProductCode())
                .orElseThrow(() -> new ProductNotFoundException(application.getProductCode()));

        if (Boolean.TRUE.equals(product.getAllowMultipleAccounts())) {
            return passed("Product allows multiple accounts");
        }

        boolean existingAccount = bankAccountRepository
                .existsByApplicationCustomerIdAndApplicationProductCodeAndApplicationApplicationIdNot(
                        application.getCustomerId(),
                        application.getProductCode(),
                        application.getApplicationId()
                );

        return existingAccount
                ? new RuleResult(
                        ApplicationRuleCode.DUPLICATE_PRODUCT,
                        false,
                        "Customer already owns an account for this product"
                )
                : passed("Customer does not own an account for this product");
    }

    private RuleResult passed(String message) {
        return new RuleResult(ApplicationRuleCode.DUPLICATE_PRODUCT, true, message);
    }
}
