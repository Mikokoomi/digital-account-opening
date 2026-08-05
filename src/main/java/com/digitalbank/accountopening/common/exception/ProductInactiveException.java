package com.digitalbank.accountopening.common.exception;

public class ProductInactiveException extends RuntimeException {

    public ProductInactiveException(String productCode) {
        super("Product is inactive: " + productCode);
    }
}
