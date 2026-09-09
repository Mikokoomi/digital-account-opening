package com.digitalbank.accountopening.bankaccount;

import com.digitalbank.accountopening.application.*;
import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import com.digitalbank.accountopening.application.workflow.ApplicationWorkflowService;
import com.digitalbank.accountopening.bankaccount.dto.AccountProvisioningResponse;
import com.digitalbank.accountopening.common.exception.*;
import com.digitalbank.accountopening.integration.corebanking.*;
import com.digitalbank.accountopening.integration.tracking.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class AccountProvisioningStateService {
    private static final IntegrationType TYPE = IntegrationType.CORE_BANKING_CREATE_ACCOUNT;
    private final AccountApplicationRepository applications;
    private final BankAccountRepository accounts;
    private final IntegrationRequestRepository integrations;
    private final ApplicationWorkflowService workflow;
    private final Clock clock;

    public AccountProvisioningStateService(AccountApplicationRepository applications, BankAccountRepository accounts,
            IntegrationRequestRepository integrations, ApplicationWorkflowService workflow, Clock clock) {
        this.applications=applications; this.accounts=accounts; this.integrations=integrations; this.workflow=workflow; this.clock=clock;
    }

    @Transactional
    public Preparation prepareInitial(UUID applicationId) {
        AccountApplication application=find(applicationId);
        if (application.getStatus()==ApplicationStatus.COMPLETED) {
            BankAccount account=accounts.findByApplicationApplicationId(applicationId)
                    .orElseThrow(() -> new CoreBankingNonRetryableException("Completed application has no local account", null));
            return Preparation.completed(response(application, account));
        }
        if (application.getStatus()==ApplicationStatus.ACCOUNT_CREATING) throw new AccountCreationInProgressException();
        if (application.getStatus()!=ApplicationStatus.APPROVED) throw new AccountCreationNotAllowedException(application.getStatus());
        if (accounts.existsByApplicationApplicationId(applicationId)) throw new BankAccountAlreadyExistsException();
        OffsetDateTime now=OffsetDateTime.now(clock);
        IntegrationRequest integration=integrations.findByApplicationApplicationIdAndIntegrationType(applicationId, TYPE)
                .orElseGet(() -> newIntegration(application, now));
        integration.setStatus(IntegrationStatus.IN_PROGRESS); integration.setUpdatedAt(now); integrations.save(integration);
        workflow.transition(application, ApplicationStatus.ACCOUNT_CREATING, "SYSTEM", "Bank account creation started");
        applications.save(application);
        return Preparation.pending(context(application, integration));
    }

    @Transactional
    public ProvisioningContext prepareRetry(UUID applicationId) {
        AccountApplication application=find(applicationId);
        if (application.getStatus()!=ApplicationStatus.RETRY_PENDING) throw new AccountCreationRetryNotAllowedException(application.getStatus());
        IntegrationRequest integration=integration(applicationId);
        if (integration.getStatus()!=IntegrationStatus.RETRY_PENDING) throw new AccountCreationRetryNotAllowedException(application.getStatus());
        OffsetDateTime now=OffsetDateTime.now(clock); integration.setStatus(IntegrationStatus.IN_PROGRESS); integration.setUpdatedAt(now);
        workflow.transition(application, ApplicationStatus.ACCOUNT_CREATING, "SYSTEM", "Manual account creation retry started");
        applications.save(application); integrations.save(integration);
        return context(application, integration);
    }

    @Transactional
    public void beforeAttempt(UUID integrationId) {
        IntegrationRequest request=integrations.findById(integrationId).orElseThrow();
        OffsetDateTime now=OffsetDateTime.now(clock); request.setAttemptCount(request.getAttemptCount()+1);
        request.setLastAttemptAt(now); request.setUpdatedAt(now); integrations.save(request);
    }

    @Transactional
    public void recordRetryableFailure(UUID integrationId, CoreBankingRetryableException failure) {
        IntegrationRequest request=integrations.findById(integrationId).orElseThrow();
        request.setLastHttpStatus(failure.getHttpStatus()); request.setLastErrorCode("CORE_BANKING_SERVICE_UNAVAILABLE");
        request.setLastErrorMessage(limit(failure.getMessage())); request.setUpdatedAt(OffsetDateTime.now(clock)); integrations.save(request);
    }

    @Transactional
    public void exhaust(UUID applicationId, UUID integrationId) {
        AccountApplication application=find(applicationId); IntegrationRequest request=integrations.findById(integrationId).orElseThrow();
        request.setStatus(IntegrationStatus.RETRY_PENDING); request.setUpdatedAt(OffsetDateTime.now(clock)); integrations.save(request);
        workflow.transition(application, ApplicationStatus.RETRY_PENDING, "SYSTEM", "Core Banking retry attempts exhausted"); applications.save(application);
    }

    @Transactional
    public void failDefinitively(UUID applicationId, UUID integrationId, CoreBankingDefinitiveFailureException failure) {
        AccountApplication application=find(applicationId); IntegrationRequest request=integrations.findById(integrationId).orElseThrow();
        request.setStatus(IntegrationStatus.FAILED); request.setLastHttpStatus(failure.getHttpStatus());
        request.setLastErrorCode(failure instanceof CoreBankingIdempotencyConflictException
                ? "CORE_BANKING_IDEMPOTENCY_CONFLICT" : "CORE_BANKING_REQUEST_REJECTED");
        request.setLastErrorMessage(limit(failure.getMessage())); request.setUpdatedAt(OffsetDateTime.now(clock)); integrations.save(request);
        workflow.transition(application, ApplicationStatus.FAILED, "SYSTEM", "Core Banking definitively rejected account creation");
        applications.save(application);
    }

    @Transactional
    public void markAmbiguousResult(UUID applicationId, UUID integrationId, CoreBankingNonRetryableException failure) {
        moveToRetryPending(applicationId, integrationId, failure.getHttpStatus(), "CORE_BANKING_RESPONSE_INVALID",
                failure.getMessage(), "Core Banking result could not be confirmed");
    }

    @Transactional
    public void markRetryInterrupted(UUID applicationId, UUID integrationId, CoreBankingRetryableException failure) {
        moveToRetryPending(applicationId, integrationId, failure.getHttpStatus(), "CORE_BANKING_RETRY_INTERRUPTED",
                failure.getMessage(), "Core Banking retry was interrupted");
    }

    @Transactional
    public AccountProvisioningResponse complete(UUID applicationId, UUID integrationId, CoreBankingAccountResponse external) {
        AccountApplication application=find(applicationId);
        if (application.getStatus()!=ApplicationStatus.ACCOUNT_CREATING) throw new AccountCreationNotAllowedException(application.getStatus());
        BankAccount account=accounts.findByApplicationApplicationId(applicationId).orElseGet(BankAccount::new);
        if (account.getId()==null) account.setApplication(application);
        account.setExternalAccountId(external.accountId()); account.setAccountNumber(external.accountNumber());
        account.setStatus(external.status()); account.setOpenedAt(external.openedAt()); accounts.save(account);
        workflow.transition(application, ApplicationStatus.COMPLETED, "SYSTEM", "Bank account created successfully"); applications.save(application);
        IntegrationRequest integration=integrations.findById(integrationId).orElseThrow(); integration.setStatus(IntegrationStatus.SUCCEEDED);
        integration.setExternalReference(external.accountNumber()); integration.setLastHttpStatus(null);
        integration.setLastErrorCode(null); integration.setLastErrorMessage(null); integration.setUpdatedAt(OffsetDateTime.now(clock)); integrations.save(integration);
        return response(application, account);
    }

    @Transactional(readOnly=true)
    public List<IntegrationRequestResponse> getIntegrationRequests(UUID applicationId) {
        find(applicationId);
        return integrations.findByApplicationApplicationIdOrderByCreatedAtAsc(applicationId).stream().map(IntegrationRequestResponse::from).toList();
    }

    private IntegrationRequest newIntegration(AccountApplication application, OffsetDateTime now) {
        IntegrationRequest r=new IntegrationRequest(); r.setApplication(application); r.setIntegrationType(TYPE);
        r.setIdempotencyKey("CREATE_ACCOUNT:"+application.getApplicationId()); r.setStatus(IntegrationStatus.IN_PROGRESS);
        r.setAttemptCount(0); r.setCreatedAt(now); r.setUpdatedAt(now); return r;
    }
    private void moveToRetryPending(UUID applicationId, UUID integrationId, Integer httpStatus, String errorCode,
            String errorMessage, String transitionReason) {
        AccountApplication application=find(applicationId); IntegrationRequest request=integrations.findById(integrationId).orElseThrow();
        request.setStatus(IntegrationStatus.RETRY_PENDING); request.setLastHttpStatus(httpStatus);
        request.setLastErrorCode(errorCode); request.setLastErrorMessage(limit(errorMessage));
        request.setUpdatedAt(OffsetDateTime.now(clock)); integrations.save(request);
        workflow.transition(application, ApplicationStatus.RETRY_PENDING, "SYSTEM", transitionReason); applications.save(application);
    }
    private ProvisioningContext context(AccountApplication a, IntegrationRequest r) {
        return new ProvisioningContext(a.getApplicationId(),a.getCustomerId(),a.getProductCode(),r.getId(),r.getIdempotencyKey());
    }
    private IntegrationRequest integration(UUID id) { return integrations.findByApplicationApplicationIdAndIntegrationType(id,TYPE).orElseThrow(); }
    private AccountApplication find(UUID id) { return applications.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id)); }
    private AccountProvisioningResponse response(AccountApplication a, BankAccount b) { return new AccountProvisioningResponse(a.getApplicationId(),a.getStatus(),b.getAccountNumber(),b.getStatus(),b.getOpenedAt()); }
    private String limit(String value) { return value==null?null:value.substring(0,Math.min(value.length(),500)); }
    public record ProvisioningContext(UUID applicationId,String customerId,String productCode,UUID integrationId,String idempotencyKey) {}
    public record Preparation(AccountProvisioningResponse completed, ProvisioningContext context) {
        static Preparation completed(AccountProvisioningResponse response){return new Preparation(response,null);}
        static Preparation pending(ProvisioningContext context){return new Preparation(null,context);}
        public boolean alreadyCompleted(){return completed!=null;}
    }
}
