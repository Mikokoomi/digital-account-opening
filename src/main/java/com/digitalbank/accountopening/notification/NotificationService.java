package com.digitalbank.accountopening.notification;

import com.digitalbank.accountopening.application.*;
import com.digitalbank.accountopening.common.exception.ApplicationNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.time.*;
import java.util.*;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final AccountApplicationRepository applications;
    private final NotificationSender sender;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, AccountApplicationRepository applications,
            NotificationSender sender, Clock clock) {
        this.notifications = notifications; this.applications = applications; this.sender = sender; this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(NotificationRequestedEvent event) {
        AccountApplication application = findForUpdate(event.applicationId());
        if (notifications.existsByApplicationApplicationIdAndType(event.applicationId(), event.type())) return;
        Notification notification = new Notification(); notification.setApplication(application);
        notification.setCustomerId(event.customerId()); notification.setType(event.type());
        notification.setStatus(NotificationStatus.PENDING); notification.setSubject(limit(event.subject(), 200));
        notification.setMessage(limit(event.message(), 500)); notification.setCreatedAt(OffsetDateTime.now(clock));
        notifications.save(notification);
        try {
            sender.send(new NotificationMessage(event.applicationId(), event.customerId(), event.type(),
                    notification.getSubject(), notification.getMessage()));
            notification.setStatus(NotificationStatus.SENT); notification.setSentAt(OffsetDateTime.now(clock));
        } catch (RuntimeException failure) {
            notification.setStatus(NotificationStatus.FAILED); notification.setFailureReason(limit(failure.getMessage(), 500));
        }
        notifications.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getByApplication(UUID applicationId) {
        find(applicationId);
        return notifications.findAllByApplicationApplicationIdOrderByCreatedAtAsc(applicationId)
                .stream().map(NotificationResponse::from).toList();
    }

    private AccountApplication find(UUID id) { return applications.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id)); }
    private AccountApplication findForUpdate(UUID id) { return applications.findByIdForUpdate(id).orElseThrow(() -> new ApplicationNotFoundException(id)); }
    private String limit(String value, int length) { return value == null ? null : value.substring(0, Math.min(value.length(), length)); }
}
