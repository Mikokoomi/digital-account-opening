package com.digitalbank.accountopening.bankaccount;

import com.digitalbank.accountopening.bankaccount.dto.AccountProvisioningResponse;
import com.digitalbank.accountopening.integration.corebanking.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class ApplicationAccountProvisioningService {
    private final AccountProvisioningStateService stateService;
    private final CoreBankingClient coreBankingClient;
    private final int maxAttempts;
    private final long backoffMs;
    public ApplicationAccountProvisioningService(AccountProvisioningStateService stateService, CoreBankingClient coreBankingClient,
            @Value("${integration.core-banking.retry.max-attempts:3}") int maxAttempts,
            @Value("${integration.core-banking.retry.backoff-ms:200}") long backoffMs) {
        this.stateService=stateService; this.coreBankingClient=coreBankingClient;
        if(maxAttempts<1) throw new IllegalArgumentException("maxAttempts must be positive");
        this.maxAttempts=maxAttempts; this.backoffMs=Math.max(0,backoffMs);
    }
    public AccountProvisioningResponse createAccount(UUID applicationId) {
        var preparation=stateService.prepareInitial(applicationId);
        return preparation.alreadyCompleted()?preparation.completed():execute(preparation.context());
    }
    public AccountProvisioningResponse retryAccountCreation(UUID applicationId) { return execute(stateService.prepareRetry(applicationId)); }
    private AccountProvisioningResponse execute(AccountProvisioningStateService.ProvisioningContext context) {
        CoreBankingRetryableException last=null;
        for(int attempt=1;attempt<=maxAttempts;attempt++) {
            stateService.beforeAttempt(context.integrationId());
            try {
                var result=coreBankingClient.createAccount(context.idempotencyKey(),new CoreBankingCreateAccountRequest(
                        context.applicationId(),context.customerId(),context.productCode()));
                return stateService.complete(context.applicationId(),context.integrationId(),result);
            } catch(CoreBankingRetryableException failure) {
                last=failure; stateService.recordRetryableFailure(context.integrationId(),failure);
                if(attempt<maxAttempts) backoff();
            } catch(CoreBankingNonRetryableException failure) {
                stateService.fail(context.integrationId(),failure); throw failure;
            }
        }
        stateService.exhaust(context.applicationId(),context.integrationId()); throw last;
    }
    private void backoff() {
        try { Thread.sleep(backoffMs); }
        catch(InterruptedException exception) { Thread.currentThread().interrupt(); throw new CoreBankingRetryableException("Retry interrupted",null,exception); }
    }
}
