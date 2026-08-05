package com.digitalbank.accountopening.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApplicationRequest(
        @NotBlank @Size(max = 100) String customerId,
        @NotBlank @Size(max = 50) String productCode
) {
}
