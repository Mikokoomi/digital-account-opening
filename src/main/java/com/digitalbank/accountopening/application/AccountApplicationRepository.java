package com.digitalbank.accountopening.application;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface AccountApplicationRepository extends JpaRepository<AccountApplication, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select application from AccountApplication application where application.applicationId = :applicationId")
    Optional<AccountApplication> findByIdForUpdate(@Param("applicationId") UUID applicationId);
}
