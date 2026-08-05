package com.digitalbank.accountopening.application;

import com.digitalbank.accountopening.application.dto.ApplicationResponse;
import com.digitalbank.accountopening.application.dto.CreateApplicationRequest;
import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import com.digitalbank.accountopening.common.exception.ApplicationNotFoundException;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private Product activeProduct() {
        Product product = new Product();
        product.setProductCode("CURRENT_ACCOUNT");
        product.setProductName("Current Account");
        product.setActive(true);
        return product;
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
