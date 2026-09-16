package com.digitalbank.accountopening.application;

import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface AccountApplicationRepository extends JpaRepository<AccountApplication, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select application from AccountApplication application where application.applicationId = :applicationId")
    Optional<AccountApplication> findByIdForUpdate(@Param("applicationId") UUID applicationId);

    boolean existsByCustomerIdAndProductCodeAndApplicationIdNotAndStatusIn(
            String customerId,
            String productCode,
            UUID applicationId,
            Collection<ApplicationStatus> statuses
    );
}
