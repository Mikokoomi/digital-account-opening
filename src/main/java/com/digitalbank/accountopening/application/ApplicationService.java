package com.digitalbank.accountopening.application;

import com.digitalbank.accountopening.application.dto.ApplicationResponse;
import com.digitalbank.accountopening.application.dto.CreateApplicationRequest;
import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import com.digitalbank.accountopening.common.exception.ApplicationNotFoundException;
import com.digitalbank.accountopening.common.exception.ProductInactiveException;
import com.digitalbank.accountopening.common.exception.ProductNotFoundException;
import com.digitalbank.accountopening.product.Product;
import com.digitalbank.accountopening.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        AccountApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
        Product product = productRepository.findByProductCode(application.getProductCode())
                .orElseThrow(() -> new ProductNotFoundException(application.getProductCode()));

        return applicationMapper.toResponse(application, product);
    }
}
