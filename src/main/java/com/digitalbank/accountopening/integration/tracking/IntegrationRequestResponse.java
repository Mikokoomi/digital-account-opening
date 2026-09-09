package com.digitalbank.accountopening.integration.tracking;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IntegrationRequestResponse(UUID id, UUID applicationId, IntegrationType integrationType,
        IntegrationStatus status, String idempotencyKey, int attemptCount, Integer lastHttpStatus,
        String externalReference, String lastErrorCode, String lastErrorMessage,
        OffsetDateTime lastAttemptAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static IntegrationRequestResponse from(IntegrationRequest r) {
        return new IntegrationRequestResponse(r.getId(), r.getApplication().getApplicationId(), r.getIntegrationType(),
                r.getStatus(), r.getIdempotencyKey(), r.getAttemptCount(), r.getLastHttpStatus(),
                r.getExternalReference(), r.getLastErrorCode(), r.getLastErrorMessage(),
                r.getLastAttemptAt(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
