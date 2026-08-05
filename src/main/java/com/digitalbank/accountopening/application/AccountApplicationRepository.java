package com.digitalbank.accountopening.application;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountApplicationRepository extends JpaRepository<AccountApplication, UUID> {
}
