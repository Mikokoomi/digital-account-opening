package com.digitalbank.accountopening.audit;

import com.digitalbank.accountopening.application.*;
import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import com.digitalbank.accountopening.common.exception.ApplicationNotFoundException;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditLogServiceTest {
    AuditLogRepository logs = mock(AuditLogRepository.class);
    AccountApplicationRepository applications = mock(AccountApplicationRepository.class);
    Clock clock = Clock.fixed(Instant.parse("2026-09-09T08:00:00Z"), ZoneOffset.UTC);
    AuditLogService service = new AuditLogService(logs, applications, clock);

    @Test void recordsBusinessActionWithDeterministicTimestamp() {
        AccountApplication application = application();
        service.record(application, AuditActorType.CUSTOMER, "CUS001", AuditAction.APPLICATION_CREATED,
                "APPLICATION", application.getApplicationId().toString(), AuditResult.SUCCESS, "Application created");
        verify(logs).save(argThat(log -> log.getApplication() == application
                && log.getActorType() == AuditActorType.CUSTOMER
                && log.getAction() == AuditAction.APPLICATION_CREATED
                && log.getCreatedAt().equals(OffsetDateTime.now(clock))));
    }

    @Test void queryReturnsRepositoryOrder() {
        AccountApplication application = application(); AuditLog first = log(application, AuditAction.APPLICATION_CREATED);
        AuditLog second = log(application, AuditAction.APPLICATION_SUBMITTED);
        when(applications.findById(application.getApplicationId())).thenReturn(Optional.of(application));
        when(logs.findAllByApplicationApplicationIdOrderByCreatedAtAsc(application.getApplicationId()))
                .thenReturn(List.of(first, second));
        List<AuditLogResponse> result = service.getByApplication(application.getApplicationId());
        assertEquals(List.of(AuditAction.APPLICATION_CREATED, AuditAction.APPLICATION_SUBMITTED),
                result.stream().map(AuditLogResponse::action).toList());
    }

    @Test void queryRejectsUnknownApplication() {
        UUID id = UUID.randomUUID(); when(applications.findById(id)).thenReturn(Optional.empty());
        assertThrows(ApplicationNotFoundException.class, () -> service.getByApplication(id));
    }

    private AccountApplication application() { AccountApplication a = new AccountApplication(); a.setApplicationId(UUID.randomUUID()); a.setCustomerId("CUS001"); a.setStatus(ApplicationStatus.DRAFT); return a; }
    private AuditLog log(AccountApplication a, AuditAction action) { AuditLog log = new AuditLog(); log.setId(UUID.randomUUID()); log.setApplication(a); log.setActorType(AuditActorType.SYSTEM); log.setActorId("SYSTEM"); log.setAction(action); log.setEntityType("APPLICATION"); log.setEntityId(a.getApplicationId().toString()); log.setResult(AuditResult.SUCCESS); log.setCreatedAt(OffsetDateTime.now(clock)); return log; }
}
