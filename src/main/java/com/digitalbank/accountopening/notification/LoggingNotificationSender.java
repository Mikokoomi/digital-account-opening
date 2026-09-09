package com.digitalbank.accountopening.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingNotificationSender implements NotificationSender {
    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);
    @Override public void send(NotificationMessage message) {
        log.info("Notification delivered: type={}, applicationId={}", message.type(), message.applicationId());
    }
}
