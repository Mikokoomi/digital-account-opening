package com.digitalbank.accountopening.notification;

import java.util.UUID;

public record NotificationMessage(UUID applicationId, String customerId, NotificationType type,
        String subject, String message) { }
