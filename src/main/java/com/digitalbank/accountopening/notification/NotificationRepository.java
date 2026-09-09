package com.digitalbank.accountopening.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findAllByApplicationApplicationIdOrderByCreatedAtAsc(UUID applicationId);
    boolean existsByApplicationApplicationIdAndType(UUID applicationId, NotificationType type);
}
