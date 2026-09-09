package com.digitalbank.accountopening.bankaccount;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {
    boolean existsByApplicationApplicationId(UUID applicationId);
    Optional<BankAccount> findByApplicationApplicationId(UUID applicationId);
}
