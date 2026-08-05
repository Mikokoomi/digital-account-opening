package com.digitalbank.accountopening.application;

import com.digitalbank.accountopening.application.dto.ApplicationResponse;
import com.digitalbank.accountopening.application.dto.CreateApplicationRequest;
import com.digitalbank.accountopening.application.dto.UpdateApplicationRequest;
import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import com.digitalbank.accountopening.common.exception.ApplicationNotEditableException;
import com.digitalbank.accountopening.common.exception.ApplicationNotFoundException;
import com.digitalbank.accountopening.common.exception.ApplicationNotSubmittableException;
import com.digitalbank.accountopening.common.exception.ProductInactiveException;
import com.digitalbank.accountopening.common.exception.ProductNotFoundException;
import com.digitalbank.accountopening.product.Product;
import com.digitalbank.accountopening.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ApplicationService {

    private final AccountApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final ProductRepository productRepository;
    private final AccountApplicationMapper applicationMapper;

    public ApplicationService(
            AccountApplicationRepository applicationRepository,
            ApplicationStatusHistoryRepository historyRepository,
            ProductRepository productRepository,
            AccountApplicationMapper applicationMapper
    ) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.productRepository = productRepository;
        this.applicationMapper = applicationMapper;
    }

    @Transactional
    public ApplicationResponse createApplication(CreateApplicationRequest request) {
        String customerId = request.customerId().trim();
        String productCode = request.productCode().trim();
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ProductNotFoundException(productCode));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new ProductInactiveException(productCode);
        }

        AccountApplication application = new AccountApplication();
        application.setCustomerId(customerId);
        application.setProductCode(product.getProductCode());
        application.setStatus(ApplicationStatus.DRAFT);

        AccountApplication savedApplication = applicationRepository.save(application);

        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setApplication(savedApplication);
        history.setFromStatus(null);
        history.setToStatus(ApplicationStatus.DRAFT);
        history.setChangedBy(customerId);
        history.setReason("Application created");
        historyRepository.save(history);

        return applicationMapper.toResponse(savedApplication, product);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplication(UUID applicationId) {
        AccountApplication application = findApplication(applicationId);
        Product product = findProduct(application.getProductCode());

        return applicationMapper.toResponse(application, product);
    }

    @Transactional
    public ApplicationResponse updateApplication(
            UUID applicationId,
            UpdateApplicationRequest request
    ) {
        AccountApplication application = findApplication(applicationId);
        if (application.getStatus() != ApplicationStatus.DRAFT) {
            throw new ApplicationNotEditableException();
        }

        String productCode = request.productCode().trim();
        Product product = findActiveProduct(productCode);
        application.setProductCode(product.getProductCode());

        return applicationMapper.toResponse(application, product);
    }

    @Transactional
    public ApplicationResponse submitApplication(UUID applicationId) {
        AccountApplication application = findApplication(applicationId);
        if (application.getStatus() != ApplicationStatus.DRAFT) {
            throw new ApplicationNotSubmittableException();
        }

        Product product = findActiveProduct(application.getProductCode());
        ApplicationStatus oldStatus = application.getStatus();
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setSubmittedAt(OffsetDateTime.now());

        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setApplication(application);
        history.setFromStatus(oldStatus);
        history.setToStatus(ApplicationStatus.SUBMITTED);
        history.setChangedBy(application.getCustomerId());
        history.setReason("Application submitted");
        historyRepository.save(history);

        return applicationMapper.toResponse(application, product);
    }

    private AccountApplication findApplication(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
    }

    private Product findProduct(String productCode) {
        return productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ProductNotFoundException(productCode));
    }

    private Product findActiveProduct(String productCode) {
        Product product = findProduct(productCode);
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new ProductInactiveException(productCode);
        }
        return product;
    }
}
