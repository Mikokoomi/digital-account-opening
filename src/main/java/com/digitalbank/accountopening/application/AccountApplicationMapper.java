package com.digitalbank.accountopening.application;

import com.digitalbank.accountopening.application.dto.ApplicationResponse;
import com.digitalbank.accountopening.product.Product;
import org.springframework.stereotype.Component;

@Component
public class AccountApplicationMapper {

    public ApplicationResponse toResponse(AccountApplication application, Product product) {
        return new ApplicationResponse(
                application.getApplicationId(),
                application.getCustomerId(),
                application.getProductCode(),
                product.getProductName(),
                application.getStatus(),
                application.getKycStatus(),
                application.getCifVerifiedAt(),
                application.getRejectReason(),
                application.getSubmittedAt(),
                application.getCancelledAt(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}
