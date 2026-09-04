package com.digitalbank.accountopening.bankaccount;

import com.digitalbank.accountopening.bankaccount.dto.AccountProvisioningResponse;
import com.digitalbank.accountopening.integration.corebanking.*;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class ApplicationAccountProvisioningService {
    private final AccountProvisioningStateService stateService;
    private final CoreBankingClient coreBankingClient;
    public ApplicationAccountProvisioningService(AccountProvisioningStateService stateService, CoreBankingClient coreBankingClient) {
        this.stateService=stateService; this.coreBankingClient=coreBankingClient;
    }
    public AccountProvisioningResponse createAccount(UUID applicationId) {
        AccountProvisioningStateService.ProvisioningApplication application=stateService.start(applicationId);
        CoreBankingAccountResponse result=coreBankingClient.createAccount(new CoreBankingCreateAccountRequest(
                application.applicationId(), application.customerId(), application.productCode()));
        return stateService.complete(applicationId, result);
    }
}
