package com.digitalbank.accountopening.audit;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogResponse(UUID id, AuditActorType actorType, String actorId, AuditAction action,
        String entityType, String entityId, UUID applicationId, AuditResult result, String details,
        OffsetDateTime createdAt) {
    static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getActorType(), log.getActorId(), log.getAction(),
                log.getEntityType(), log.getEntityId(), log.getApplication().getApplicationId(),
                log.getResult(), log.getDetails(), log.getCreatedAt());
    }
}
