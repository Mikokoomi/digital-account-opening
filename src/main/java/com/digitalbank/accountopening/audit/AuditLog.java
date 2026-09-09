package com.digitalbank.accountopening.audit;

import com.digitalbank.accountopening.application.AccountApplication;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Enumerated(EnumType.STRING) @Column(name = "actor_type", nullable = false, length = 20)
    private AuditActorType actorType;
    @Column(name = "actor_id", nullable = false, length = 100)
    private String actorId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 60)
    private AuditAction action;
    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType;
    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private AccountApplication application;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private AuditResult result;
    @Column(length = 500)
    private String details;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
