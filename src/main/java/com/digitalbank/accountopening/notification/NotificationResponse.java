package com.digitalbank.accountopening.notification;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(UUID id, UUID applicationId, String customerId, NotificationType type,
        NotificationStatus status, String subject, String message, OffsetDateTime createdAt,
        OffsetDateTime sentAt, String failureReason) {
    static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getApplication().getApplicationId(),
                notification.getCustomerId(), notification.getType(), notification.getStatus(),
                notification.getSubject(), notification.getMessage(), notification.getCreatedAt(),
                notification.getSentAt(), notification.getFailureReason());
    }
}
