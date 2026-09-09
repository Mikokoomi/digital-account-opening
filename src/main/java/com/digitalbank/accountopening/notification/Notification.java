package com.digitalbank.accountopening.notification;

import com.digitalbank.accountopening.application.AccountApplication;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, updatable = false)
    private AccountApplication application;
    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private NotificationType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private NotificationStatus status;
    @Column(length = 200)
    private String subject;
    @Column(nullable = false, length = 500)
    private String message;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "sent_at")
    private OffsetDateTime sentAt;
    @Column(name = "failure_reason", length = 500)
    private String failureReason;
}
