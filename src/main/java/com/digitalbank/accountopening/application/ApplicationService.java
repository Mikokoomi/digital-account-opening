package com.digitalbank.accountopening.application;

import com.digitalbank.accountopening.application.dto.ApplicationRuleEvaluationResponse;
import com.digitalbank.accountopening.application.rule.ApplicationRuleEvaluationService;
import com.digitalbank.accountopening.application.rule.RuleEvaluationResult;

import com.digitalbank.accountopening.application.dto.ApplicationResponse;
import com.digitalbank.accountopening.application.dto.ApplicationHistoryResponse;
import com.digitalbank.accountopening.application.dto.ApplicationKycVerificationResponse;
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
import com.digitalbank.accountopening.integration.cifkyc.CifKycVerificationResult;
import com.digitalbank.accountopening.integration.cifkyc.CifKycVerificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.digitalbank.accountopening.common.exception.KycAlreadyVerifiedException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ApplicationService {

    private final AccountApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final ProductRepository productRepository;
    private final AccountApplicationMapper applicationMapper;
    private final ApplicationRuleEvaluationService applicationRuleEvaluationService;
    private final CifKycVerificationService cifKycVerificationService;

    private final Clock clock;

    public ApplicationService(
            AccountApplicationRepository applicationRepository,
            ApplicationStatusHistoryRepository historyRepository,
            ProductRepository productRepository,
            AccountApplicationMapper applicationMapper,
            CifKycVerificationService cifKycVerificationService,
            ApplicationRuleEvaluationService applicationRuleEvaluationService,
            Clock clock
    ) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.productRepository = productRepository;
        this.applicationMapper = applicationMapper;
        this.cifKycVerificationService = cifKycVerificationService;
        this.applicationRuleEvaluationService = applicationRuleEvaluationService;
        this.clock = clock;
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

    @Transactional
    public ApplicationResponse cancelApplication(UUID applicationId) {
        AccountApplication application = findApplication(applicationId);
        ApplicationStatus previousStatus = application.getStatus();
        if (previousStatus != ApplicationStatus.DRAFT && previousStatus != ApplicationStatus.SUBMITTED) {
            throw new ApplicationNotCancellableException();
        }

        application.setStatus(ApplicationStatus.CANCELLED);
        application.setCancelledAt(OffsetDateTime.now());

        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setApplication(application);
        history.setFromStatus(previousStatus);
        history.setToStatus(ApplicationStatus.CANCELLED);
        history.setChangedBy(application.getCustomerId());
        history.setReason("Application cancelled");
        historyRepository.save(history);

        Product product = findProduct(application.getProductCode());
        return applicationMapper.toResponse(application, product);
    }


    @Transactional(readOnly = true)
    public List<ApplicationHistoryResponse> getApplicationHistory(UUID applicationId) {
        findApplication(applicationId);
        return historyRepository
                .findAllByApplicationApplicationIdOrderByChangedAtAsc(applicationId)
                .stream()
                .map(applicationMapper::toHistoryResponse)
                .toList();
    }

    @Transactional
    public ApplicationKycVerificationResponse verifyCifKyc(UUID applicationId) {
    AccountApplication application = findApplication(applicationId);

    if ("VERIFIED".equals(application.getKycStatus())
            && application.getCifVerifiedAt() != null) {
        throw new KycAlreadyVerifiedException();
    }

    CifKycVerificationResult verificationResult = cifKycVerificationService.verify(
            application.getCustomerId()
    );

        application.setKycStatus(verificationResult.kycStatus());
        application.setCifVerifiedAt(OffsetDateTime.now(clock));
        applicationRepository.save(application);

        return new ApplicationKycVerificationResponse(
                applicationId,
                verificationResult.customerId(),
                verificationResult.eligible(),
                verificationResult.customerStatus(),
                verificationResult.kycStatus(),
                verificationResult.kycExpiryDate()
        );
    }

    @Transactional(readOnly = true)
    public ApplicationRuleEvaluationResponse evaluateRules(UUID applicationId) {
        AccountApplication application = findApplication(applicationId);

        RuleEvaluationResult result =
                applicationRuleEvaluationService.evaluate(application);

        return new ApplicationRuleEvaluationResponse(
                applicationId,
                result.eligible(),
                result.ruleResults(),
                result.failedRules()
        );
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
