package com.digitalbank.accountopening.bankaccount;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {
    boolean existsByApplicationApplicationId(UUID applicationId);
}
