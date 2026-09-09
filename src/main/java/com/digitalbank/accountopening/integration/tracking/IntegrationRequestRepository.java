package com.digitalbank.accountopening.integration.tracking;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface IntegrationRequestRepository extends JpaRepository<IntegrationRequest, UUID> {
    Optional<IntegrationRequest> findByApplicationApplicationIdAndIntegrationType(UUID applicationId, IntegrationType type);
    List<IntegrationRequest> findByApplicationApplicationIdOrderByCreatedAtAsc(UUID applicationId);
}
