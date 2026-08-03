package com.digitalbank.accountopening.product;

import com.digitalbank.accountopening.common.exception.ProductNotFoundException;
import com.digitalbank.accountopening.product.dto.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    @Test
    void getActiveProducts_shouldReturnProductList() {
        Product product = createProduct();

        when(productRepository.findAllByActiveTrueOrderByProductNameAsc())
                .thenReturn(List.of(product));

        List<ProductResponse> result =
                productService.getActiveProducts();

        assertEquals(1, result.size());
        assertEquals(
                "CURRENT_ACCOUNT",
                result.getFirst().productCode()
        );
        assertEquals(
                "Current Account",
                result.getFirst().productName()
        );
    }

    @Test
    void getProductByCode_shouldReturnProduct_whenProductExists() {
        Product product = createProduct();

        when(productRepository
                .findByProductCodeAndActiveTrue("CURRENT_ACCOUNT"))
                .thenReturn(Optional.of(product));

        ProductResponse result =
                productService.getProductByCode("CURRENT_ACCOUNT");

        assertEquals("CURRENT_ACCOUNT", result.productCode());
        assertEquals("VND", result.currency());
        assertEquals(18, result.minAge());
    }

    @Test
    void getProductByCode_shouldThrowException_whenProductDoesNotExist() {
        when(productRepository.findByProductCodeAndActiveTrue("ABC"))
                .thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> productService.getProductByCode("ABC")
        );

        assertEquals(
                "Product not found: ABC",
                exception.getMessage()
        );
    }

    private Product createProduct() {
        Product product = new Product();

        product.setId(1L);
        product.setProductCode("CURRENT_ACCOUNT");
        product.setProductName("Current Account");
        product.setDescription("Tài khoản thanh toán");
        product.setCurrency("VND");
        product.setMinAge(18);
        product.setAllowMultipleAccounts(false);
        product.setRequiresManualReview(false);
        product.setActive(true);

        return product;
    }
}