package com.digitalbank.accountopening.notification;

import com.digitalbank.accountopening.application.*;
import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import com.digitalbank.accountopening.common.exception.ApplicationNotFoundException;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {
    NotificationRepository notifications = mock(NotificationRepository.class);
    AccountApplicationRepository applications = mock(AccountApplicationRepository.class);
    NotificationSender sender = mock(NotificationSender.class);
    Clock clock = Clock.fixed(Instant.parse("2026-09-09T08:00:00Z"), ZoneOffset.UTC);
    NotificationService service = new NotificationService(notifications, applications, sender, clock);
    AccountApplication application;

    @BeforeEach void setup() { application = new AccountApplication(); application.setApplicationId(UUID.randomUUID()); application.setCustomerId("CUS001"); application.setStatus(ApplicationStatus.APPROVED); when(applications.findById(application.getApplicationId())).thenReturn(Optional.of(application)); when(applications.findByIdForUpdate(application.getApplicationId())).thenReturn(Optional.of(application)); }

    @Test void senderSuccessMarksNotificationSent() {
        service.handle(event(NotificationType.APPLICATION_APPROVED));
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(2)).save(captor.capture());
        Notification saved = captor.getAllValues().getLast();
        assertEquals(NotificationStatus.SENT, saved.getStatus());
        assertEquals(OffsetDateTime.now(clock), saved.getSentAt());
    }

    @Test void senderFailureMarksNotificationFailedWithoutEscaping() {
        doThrow(new IllegalStateException("delivery unavailable")).when(sender).send(any());
        assertDoesNotThrow(() -> service.handle(event(NotificationType.APPLICATION_REJECTED)));
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(2)).save(captor.capture());
        Notification saved = captor.getAllValues().getLast();
        assertEquals(NotificationStatus.FAILED, saved.getStatus());
        assertEquals("delivery unavailable", saved.getFailureReason());
    }

    @Test void duplicateBusinessEventIsIgnored() {
        when(notifications.existsByApplicationApplicationIdAndType(application.getApplicationId(), NotificationType.ACCOUNT_OPENED)).thenReturn(true);
        service.handle(event(NotificationType.ACCOUNT_OPENED));
        verify(applications).findByIdForUpdate(application.getApplicationId());
        verify(notifications, never()).save(any()); verifyNoInteractions(sender);
    }

    @Test void sequentialDuplicateEventsCreateAndSendOnlyOnce() {
        when(notifications.existsByApplicationApplicationIdAndType(application.getApplicationId(), NotificationType.ACCOUNT_OPENED))
                .thenReturn(false, true);

        service.handle(event(NotificationType.ACCOUNT_OPENED));
        service.handle(event(NotificationType.ACCOUNT_OPENED));

        verify(applications, times(2)).findByIdForUpdate(application.getApplicationId());
        verify(notifications, times(2)).save(any(Notification.class));
        verify(sender).send(any(NotificationMessage.class));
    }

    @Test void sameApplicationAllowsDifferentNotificationTypes() {
        service.handle(event(NotificationType.APPLICATION_APPROVED));
        service.handle(event(NotificationType.ACCOUNT_OPENED));

        verify(notifications).existsByApplicationApplicationIdAndType(application.getApplicationId(), NotificationType.APPLICATION_APPROVED);
        verify(notifications).existsByApplicationApplicationIdAndType(application.getApplicationId(), NotificationType.ACCOUNT_OPENED);
        verify(sender, times(2)).send(any(NotificationMessage.class));
    }

    @Test void differentApplicationsAllowSameNotificationType() {
        AccountApplication other = new AccountApplication();
        other.setApplicationId(UUID.randomUUID()); other.setCustomerId("CUS002"); other.setStatus(ApplicationStatus.APPROVED);
        when(applications.findByIdForUpdate(other.getApplicationId())).thenReturn(Optional.of(other));

        service.handle(event(NotificationType.ACCOUNT_OPENED));
        service.handle(new NotificationRequestedEvent(other.getApplicationId(), other.getCustomerId(),
                NotificationType.ACCOUNT_OPENED, "Subject", "Message"));

        verify(sender, times(2)).send(any(NotificationMessage.class));
    }

    @Test void queryReturnsOldestFirstFromRepository() {
        Notification first = notification(NotificationType.APPLICATION_APPROVED, OffsetDateTime.now(clock));
        Notification second = notification(NotificationType.ACCOUNT_OPENED, OffsetDateTime.now(clock).plusSeconds(1));
        when(notifications.findAllByApplicationApplicationIdOrderByCreatedAtAsc(application.getApplicationId())).thenReturn(List.of(first, second));
        assertEquals(List.of(NotificationType.APPLICATION_APPROVED, NotificationType.ACCOUNT_OPENED),
                service.getByApplication(application.getApplicationId()).stream().map(NotificationResponse::type).toList());
    }

    @Test void queryRejectsUnknownApplication() {
        UUID id = UUID.randomUUID(); when(applications.findById(id)).thenReturn(Optional.empty());
        assertThrows(ApplicationNotFoundException.class, () -> service.getByApplication(id));
    }

    private NotificationRequestedEvent event(NotificationType type) { return new NotificationRequestedEvent(application.getApplicationId(), application.getCustomerId(), type, "Subject", "Message"); }
    private Notification notification(NotificationType type, OffsetDateTime createdAt) { Notification n = new Notification(); n.setId(UUID.randomUUID()); n.setApplication(application); n.setCustomerId("CUS001"); n.setType(type); n.setStatus(NotificationStatus.SENT); n.setMessage("Message"); n.setCreatedAt(createdAt); n.setSentAt(createdAt); return n; }
}
