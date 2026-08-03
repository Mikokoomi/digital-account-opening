package com.digitalbank.accountopening.product.dto;

public record ProductResponse(
        String productCode,
        String productName,
        String description,
        String currency,
        Integer minAge,
        Boolean allowMultipleAccounts,
        Boolean requiresManualReview
) {
}