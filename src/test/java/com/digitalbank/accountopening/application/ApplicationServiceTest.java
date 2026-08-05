package com.digitalbank.accountopening.application;

import com.digitalbank.accountopening.application.dto.ApplicationResponse;
import com.digitalbank.accountopening.application.dto.ApplicationHistoryResponse;
import com.digitalbank.accountopening.application.dto.CreateApplicationRequest;
import com.digitalbank.accountopening.application.dto.UpdateApplicationRequest;
import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import com.digitalbank.accountopening.common.exception.ApplicationNotEditableException;
import com.digitalbank.accountopening.common.exception.ApplicationNotCancellableException;
import com.digitalbank.accountopening.common.exception.ApplicationNotFoundException;
import com.digitalbank.accountopening.common.exception.ApplicationNotSubmittableException;
import com.digitalbank.accountopening.common.exception.ProductInactiveException;
import com.digitalbank.accountopening.common.exception.ProductNotFoundException;
import com.digitalbank.accountopening.product.Product;
import com.digitalbank.accountopening.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private AccountApplicationRepository applicationRepository;

    @Mock
    private ApplicationStatusHistoryRepository historyRepository;

    @Mock
    private ProductRepository productRepository;

    private ApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new ApplicationService(
                applicationRepository,
                historyRepository,
                productRepository,
                new AccountApplicationMapper()
        );
    }

    @Test
    void createApplication_shouldPersistDraftAndInitialHistory() {
        Product product = activeProduct();
        AccountApplication savedApplication = application(
                UUID.randomUUID(),
                "CUSTOMER-001",
                "CURRENT_ACCOUNT"
        );
        when(productRepository.findByProductCode("CURRENT_ACCOUNT")).thenReturn(Optional.of(product));
        when(applicationRepository.save(any(AccountApplication.class))).thenReturn(savedApplication);

        ApplicationResponse result = applicationService.createApplication(
                new CreateApplicationRequest(" CUSTOMER-001 ", " CURRENT_ACCOUNT ")
        );

        ArgumentCaptor<AccountApplication> applicationCaptor = ArgumentCaptor.forClass(AccountApplication.class);
        ArgumentCaptor<ApplicationStatusHistory> historyCaptor = ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(applicationRepository).save(applicationCaptor.capture());
        verify(historyRepository).save(historyCaptor.capture());

        assertEquals("CUSTOMER-001", applicationCaptor.getValue().getCustomerId());
        assertEquals("CURRENT_ACCOUNT", applicationCaptor.getValue().getProductCode());
        assertEquals(ApplicationStatus.DRAFT, applicationCaptor.getValue().getStatus());
        assertEquals(savedApplication, historyCaptor.getValue().getApplication());
        assertEquals(null, historyCaptor.getValue().getFromStatus());
        assertEquals(ApplicationStatus.DRAFT, historyCaptor.getValue().getToStatus());
        assertEquals("CUSTOMER-001", historyCaptor.getValue().getChangedBy());
        assertEquals("Application created", historyCaptor.getValue().getReason());
        assertEquals(savedApplication.getApplicationId(), result.applicationId());
        assertEquals("Current Account", result.productName());
        assertEquals(ApplicationStatus.DRAFT, result.status());
    }

    @Test
    void createApplication_shouldThrowWhenProductDoesNotExist() {
        when(productRepository.findByProductCode("UNKNOWN")).thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> applicationService.createApplication(new CreateApplicationRequest("CUSTOMER-001", "UNKNOWN"))
        );

        assertEquals("Product not found: UNKNOWN", exception.getMessage());
        verify(applicationRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void createApplication_shouldThrowWhenProductIsInactive() {
        Product product = activeProduct();
        product.setActive(false);
        when(productRepository.findByProductCode("CURRENT_ACCOUNT")).thenReturn(Optional.of(product));

        ProductInactiveException exception = assertThrows(
                ProductInactiveException.class,
                () -> applicationService.createApplication(new CreateApplicationRequest("CUSTOMER-001", "CURRENT_ACCOUNT"))
        );

        assertEquals("Product is inactive: CURRENT_ACCOUNT", exception.getMessage());
        verify(applicationRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void getApplication_shouldReturnMappedApplication() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(productRepository.findByProductCode("CURRENT_ACCOUNT")).thenReturn(Optional.of(activeProduct()));

        ApplicationResponse result = applicationService.getApplication(applicationId);

        assertEquals(applicationId, result.applicationId());
        assertEquals("CUSTOMER-001", result.customerId());
        assertEquals("CURRENT_ACCOUNT", result.productCode());
        assertEquals("Current Account", result.productName());
        assertEquals(ApplicationStatus.DRAFT, result.status());
    }

    @Test
    void getApplication_shouldThrowWhenApplicationDoesNotExist() {
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        ApplicationNotFoundException exception = assertThrows(
                ApplicationNotFoundException.class,
                () -> applicationService.getApplication(applicationId)
        );

        assertEquals("Application not found: " + applicationId, exception.getMessage());
    }

    @Test
    void updateApplication_shouldUpdateProductForDraftApplication() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        Product savingProduct = activeProduct("SAVING_ACCOUNT", "Saving Account");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(productRepository.findByProductCode("SAVING_ACCOUNT")).thenReturn(Optional.of(savingProduct));

        ApplicationResponse result = applicationService.updateApplication(
                applicationId,
                new UpdateApplicationRequest(" SAVING_ACCOUNT ")
        );

        assertEquals("SAVING_ACCOUNT", application.getProductCode());
        assertEquals(ApplicationStatus.DRAFT, application.getStatus());
        assertEquals("SAVING_ACCOUNT", result.productCode());
        assertEquals("Saving Account", result.productName());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void updateApplication_shouldThrowWhenApplicationDoesNotExist() {
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThrows(
                ApplicationNotFoundException.class,
                () -> applicationService.updateApplication(
                        applicationId,
                        new UpdateApplicationRequest("SAVING_ACCOUNT")
                )
        );
    }

    @Test
    void updateApplication_shouldThrowWhenApplicationIsNotDraft() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        application.setStatus(ApplicationStatus.SUBMITTED);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThrows(
                ApplicationNotEditableException.class,
                () -> applicationService.updateApplication(
                        applicationId,
                        new UpdateApplicationRequest("SAVING_ACCOUNT")
                )
        );

        assertEquals("CURRENT_ACCOUNT", application.getProductCode());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void updateApplication_shouldThrowWhenProductDoesNotExist() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(productRepository.findByProductCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> applicationService.updateApplication(applicationId, new UpdateApplicationRequest("UNKNOWN"))
        );
    }

    @Test
    void updateApplication_shouldThrowWhenProductIsInactive() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        Product product = activeProduct("SAVING_ACCOUNT", "Saving Account");
        product.setActive(false);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(productRepository.findByProductCode("SAVING_ACCOUNT")).thenReturn(Optional.of(product));

        assertThrows(
                ProductInactiveException.class,
                () -> applicationService.updateApplication(
                        applicationId,
                        new UpdateApplicationRequest("SAVING_ACCOUNT")
                )
        );

        assertEquals("CURRENT_ACCOUNT", application.getProductCode());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void submitApplication_shouldTransitionDraftToSubmittedAndPersistHistory() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(productRepository.findByProductCode("CURRENT_ACCOUNT")).thenReturn(Optional.of(activeProduct()));

        ApplicationResponse result = applicationService.submitApplication(applicationId);

        ArgumentCaptor<ApplicationStatusHistory> historyCaptor = ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertEquals(ApplicationStatus.SUBMITTED, application.getStatus());
        assertNotNull(application.getSubmittedAt());
        assertEquals(ApplicationStatus.SUBMITTED, result.status());
        assertNotNull(result.submittedAt());
        assertEquals(application, historyCaptor.getValue().getApplication());
        assertEquals(ApplicationStatus.DRAFT, historyCaptor.getValue().getFromStatus());
        assertEquals(ApplicationStatus.SUBMITTED, historyCaptor.getValue().getToStatus());
        assertEquals("CUSTOMER-001", historyCaptor.getValue().getChangedBy());
        assertEquals("Application submitted", historyCaptor.getValue().getReason());
    }

    @Test
    void submitApplication_shouldThrowWhenApplicationIsNotDraft() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        application.setStatus(ApplicationStatus.SUBMITTED);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThrows(
                ApplicationNotSubmittableException.class,
                () -> applicationService.submitApplication(applicationId)
        );

        verify(historyRepository, never()).save(any());
    }

    @Test
    void submitApplication_shouldThrowWhenApplicationDoesNotExist() {
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThrows(ApplicationNotFoundException.class, () -> applicationService.submitApplication(applicationId));
    }

    @Test
    void submitApplication_shouldThrowWhenProductIsInactive() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        Product product = activeProduct();
        product.setActive(false);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(productRepository.findByProductCode("CURRENT_ACCOUNT")).thenReturn(Optional.of(product));

        assertThrows(ProductInactiveException.class, () -> applicationService.submitApplication(applicationId));

        assertEquals(ApplicationStatus.DRAFT, application.getStatus());
        assertNull(application.getSubmittedAt());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void cancelApplication_shouldCancelDraftAndPersistHistory() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(productRepository.findByProductCode("CURRENT_ACCOUNT")).thenReturn(Optional.of(activeProduct()));

        ApplicationResponse result = applicationService.cancelApplication(applicationId);

        ArgumentCaptor<ApplicationStatusHistory> historyCaptor = ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertEquals(ApplicationStatus.CANCELLED, application.getStatus());
        assertNotNull(application.getCancelledAt());
        assertEquals(ApplicationStatus.CANCELLED, result.status());
        assertNotNull(result.cancelledAt());
        assertEquals(ApplicationStatus.DRAFT, historyCaptor.getValue().getFromStatus());
        assertEquals(ApplicationStatus.CANCELLED, historyCaptor.getValue().getToStatus());
        assertEquals("CUSTOMER-001", historyCaptor.getValue().getChangedBy());
        assertEquals("Application cancelled", historyCaptor.getValue().getReason());
    }

    @Test
    void cancelApplication_shouldCancelSubmittedAndKeepSubmittedAt() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        OffsetDateTime submittedAt = OffsetDateTime.now().minusMinutes(1);
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setSubmittedAt(submittedAt);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(productRepository.findByProductCode("CURRENT_ACCOUNT")).thenReturn(Optional.of(activeProduct()));

        applicationService.cancelApplication(applicationId);

        ArgumentCaptor<ApplicationStatusHistory> historyCaptor = ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertEquals(ApplicationStatus.CANCELLED, application.getStatus());
        assertEquals(submittedAt, application.getSubmittedAt());
        assertNotNull(application.getCancelledAt());
        assertEquals(ApplicationStatus.SUBMITTED, historyCaptor.getValue().getFromStatus());
        assertEquals(ApplicationStatus.CANCELLED, historyCaptor.getValue().getToStatus());
    }

    @Test
    void cancelApplication_shouldThrowWhenApplicationDoesNotExist() {
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThrows(ApplicationNotFoundException.class, () -> applicationService.cancelApplication(applicationId));

        verify(historyRepository, never()).save(any());
    }

    @Test
    void cancelApplication_shouldThrowWhenApplicationIsAlreadyCancelled() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        application.setStatus(ApplicationStatus.CANCELLED);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThrows(ApplicationNotCancellableException.class, () -> applicationService.cancelApplication(applicationId));

        verify(historyRepository, never()).save(any());
    }

    @Test
    void cancelApplication_shouldThrowWhenApplicationIsApproved() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        application.setStatus(ApplicationStatus.APPROVED);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThrows(ApplicationNotCancellableException.class, () -> applicationService.cancelApplication(applicationId));

        verify(historyRepository, never()).save(any());
    }

    @Test
    void getApplicationHistory_shouldReturnMappedHistoryInRepositoryOrder() {
        UUID applicationId = UUID.randomUUID();
        AccountApplication application = application(applicationId, "CUSTOMER-001", "CURRENT_ACCOUNT");
        OffsetDateTime firstChangedAt = OffsetDateTime.now().minusMinutes(2);
        OffsetDateTime secondChangedAt = OffsetDateTime.now().minusMinutes(1);
        ApplicationStatusHistory createdHistory = history(
                UUID.randomUUID(),
                application,
                null,
                ApplicationStatus.DRAFT,
                "Application created",
                firstChangedAt
        );
        ApplicationStatusHistory submittedHistory = history(
                UUID.randomUUID(),
                application,
                ApplicationStatus.DRAFT,
                ApplicationStatus.SUBMITTED,
                "Application submitted",
                secondChangedAt
        );
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(historyRepository.findAllByApplicationApplicationIdOrderByChangedAtAsc(applicationId))
                .thenReturn(List.of(createdHistory, submittedHistory));

        List<ApplicationHistoryResponse> result = applicationService.getApplicationHistory(applicationId);

        assertEquals(2, result.size());
        assertNull(result.getFirst().fromStatus());
        assertEquals(ApplicationStatus.DRAFT, result.getFirst().toStatus());
        assertEquals("Application created", result.getFirst().reason());
        assertEquals(firstChangedAt, result.getFirst().changedAt());
        assertEquals(ApplicationStatus.DRAFT, result.get(1).fromStatus());
        assertEquals(ApplicationStatus.SUBMITTED, result.get(1).toStatus());
        assertEquals(secondChangedAt, result.get(1).changedAt());
    }

    @Test
    void getApplicationHistory_shouldThrowBeforeLoadingHistoryWhenApplicationDoesNotExist() {
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThrows(ApplicationNotFoundException.class, () -> applicationService.getApplicationHistory(applicationId));

        verify(historyRepository, never()).findAllByApplicationApplicationIdOrderByChangedAtAsc(any());
    }

    private Product activeProduct() {
        return activeProduct("CURRENT_ACCOUNT", "Current Account");
    }

    private Product activeProduct(String productCode, String productName) {
        Product product = new Product();
        product.setProductCode(productCode);
        product.setProductName(productName);
        product.setActive(true);
        return product;
    }

    private ApplicationStatusHistory history(
            UUID historyId,
            AccountApplication application,
            ApplicationStatus fromStatus,
            ApplicationStatus toStatus,
            String reason,
            OffsetDateTime changedAt
    ) {
        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setHistoryId(historyId);
        history.setApplication(application);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setChangedBy(application.getCustomerId());
        history.setReason(reason);
        history.setChangedAt(changedAt);
        return history;
    }

    private AccountApplication application(UUID applicationId, String customerId, String productCode) {
        OffsetDateTime now = OffsetDateTime.now();
        AccountApplication application = new AccountApplication();
        application.setApplicationId(applicationId);
        application.setCustomerId(customerId);
        application.setProductCode(productCode);
        application.setStatus(ApplicationStatus.DRAFT);
        application.setCreatedAt(now);
        application.setUpdatedAt(now);
        return application;
    }
}
