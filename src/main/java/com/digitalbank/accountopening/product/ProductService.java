package com.digitalbank.accountopening.product;

import com.digitalbank.accountopening.common.exception.ProductNotFoundException;
import com.digitalbank.accountopening.product.dto.ProductResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> getActiveProducts() {
        return productRepository
                .findAllByActiveTrueOrderByProductNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse getProductByCode(String productCode) {
        Product product = productRepository
                .findByProductCodeAndActiveTrue(productCode)
                .orElseThrow(() -> new ProductNotFoundException(productCode));

        return toResponse(product);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getProductCode(),
                product.getProductName(),
                product.getDescription(),
                product.getCurrency(),
                product.getMinAge(),
                product.getAllowMultipleAccounts(),
                product.getRequiresManualReview()
        );
    }
}