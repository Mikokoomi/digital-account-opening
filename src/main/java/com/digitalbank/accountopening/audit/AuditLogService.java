package com.digitalbank.accountopening.audit;

import com.digitalbank.accountopening.application.*;
import com.digitalbank.accountopening.common.exception.ApplicationNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogs;
    private final AccountApplicationRepository applications;
    private final Clock clock;

    public AuditLogService(AuditLogRepository auditLogs, AccountApplicationRepository applications, Clock clock) {
        this.auditLogs = auditLogs; this.applications = applications; this.clock = clock;
    }

    @Transactional
    public void record(AccountApplication application, AuditActorType actorType, String actorId,
            AuditAction action, String entityType, String entityId, AuditResult result, String details) {
        AuditLog log = new AuditLog(); log.setActorType(actorType); log.setActorId(limit(actorId, 100));
        log.setAction(action); log.setEntityType(limit(entityType, 40)); log.setEntityId(limit(entityId, 100));
        log.setApplication(application); log.setResult(result); log.setDetails(limit(details, 500));
        log.setCreatedAt(OffsetDateTime.now(clock)); auditLogs.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID applicationId, AuditActorType actorType, String actorId,
            AuditAction action, String details) {
        AccountApplication application = find(applicationId);
        record(application, actorType, actorId, action, "APPLICATION", applicationId.toString(),
                AuditResult.FAILED, details);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getByApplication(UUID applicationId) {
        find(applicationId);
        return auditLogs.findAllByApplicationApplicationIdOrderByCreatedAtAsc(applicationId)
                .stream().map(AuditLogResponse::from).toList();
    }

    private AccountApplication find(UUID id) { return applications.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id)); }
    private String limit(String value, int length) { return value == null ? null : value.substring(0, Math.min(value.length(), length)); }
}
