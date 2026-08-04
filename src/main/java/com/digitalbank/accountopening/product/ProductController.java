package com.digitalbank.accountopening.product;

import com.digitalbank.accountopening.common.response.ApiResponse;
import com.digitalbank.accountopening.product.dto.ProductResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> getProducts() {
        return ApiResponse.success(
                "Products retrieved successfully",
                productService.getActiveProducts()
        );
    }

    @GetMapping("/{productCode}")
    public ApiResponse<ProductResponse> getProduct(
            @PathVariable String productCode
    ) {
        return ApiResponse.success(
                "Product retrieved successfully",
                productService.getProductByCode(productCode)
        );
    }
}
