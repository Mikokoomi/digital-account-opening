package com.digitalbank.accountopening.integration.tracking;

import com.digitalbank.accountopening.application.AccountApplication;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "integration_requests")
@Getter @Setter @NoArgsConstructor
public class IntegrationRequest {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private AccountApplication application;
    @Enumerated(EnumType.STRING) @Column(name = "integration_type", nullable = false, length = 50)
    private IntegrationType integrationType;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 200)
    private String idempotencyKey;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private IntegrationStatus status;
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(name = "last_http_status")
    private Integer lastHttpStatus;
    @Column(name = "external_reference", length = 100)
    private String externalReference;
    @Column(name = "last_error_code", length = 100)
    private String lastErrorCode;
    @Column(name = "last_error_message", length = 500)
    private String lastErrorMessage;
    @Column(name = "last_attempt_at")
    private OffsetDateTime lastAttemptAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
