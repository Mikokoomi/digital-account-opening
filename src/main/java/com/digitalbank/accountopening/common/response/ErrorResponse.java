package com.digitalbank.accountopening.common.response;

import java.time.LocalDateTime;

public record ErrorResponse(
        boolean success,
        String message,
        String errorCode,
        LocalDateTime timestamp
) {
}