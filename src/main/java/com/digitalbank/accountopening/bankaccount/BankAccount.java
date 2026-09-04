package com.digitalbank.accountopening.bankaccount;

import com.digitalbank.accountopening.application.AccountApplication;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Local projection of an account owned by Core Banking. */
@Entity
@Table(name = "bank_accounts")
@Getter @Setter @NoArgsConstructor
public class BankAccount {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private AccountApplication application;
    @Column(name = "external_account_id", nullable = false, unique = true)
    private UUID externalAccountId;
    @Column(name = "account_number", nullable = false, unique = true, length = 40)
    private String accountNumber;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @PrePersist void create() { createdAt = OffsetDateTime.now(); }
}
