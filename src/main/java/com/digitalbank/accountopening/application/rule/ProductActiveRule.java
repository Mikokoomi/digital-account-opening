package com.digitalbank.accountopening.application.rule;

import com.digitalbank.accountopening.application.AccountApplication;
import com.digitalbank.accountopening.common.exception.ProductNotFoundException;
import com.digitalbank.accountopening.product.Product;
import com.digitalbank.accountopening.product.ProductRepository;
import org.springframework.stereotype.Component;

@Component
public class ProductActiveRule implements ApplicationRule {

    private final ProductRepository productRepository;

    public ProductActiveRule(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public RuleResult evaluate(AccountApplication application) {
        String productCode = application.getProductCode();
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ProductNotFoundException(productCode));

        boolean active = Boolean.TRUE.equals(product.getActive());
        String message = active ? "Product is active" : "Product is inactive";
        return new RuleResult(ApplicationRuleCode.PRODUCT_ACTIVE, active, message);
    }
}
