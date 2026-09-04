package com.digitalbank.accountopening.bankaccount;

import com.digitalbank.accountopening.application.*;
import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import com.digitalbank.accountopening.application.workflow.ApplicationWorkflowService;
import com.digitalbank.accountopening.bankaccount.dto.AccountProvisioningResponse;
import com.digitalbank.accountopening.common.exception.*;
import com.digitalbank.accountopening.integration.corebanking.CoreBankingAccountResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class AccountProvisioningStateService {
    private final AccountApplicationRepository applications;
    private final BankAccountRepository accounts;
    private final ApplicationWorkflowService workflow;
    public AccountProvisioningStateService(AccountApplicationRepository applications, BankAccountRepository accounts,
                                           ApplicationWorkflowService workflow) {
        this.applications=applications; this.accounts=accounts; this.workflow=workflow;
    }
    @Transactional
    public ProvisioningApplication start(UUID applicationId) {
        AccountApplication application=find(applicationId);
        if (application.getStatus()!=ApplicationStatus.APPROVED) throw new AccountCreationNotAllowedException(application.getStatus());
        if (accounts.existsByApplicationApplicationId(applicationId)) throw new BankAccountAlreadyExistsException();
        workflow.transition(application, ApplicationStatus.ACCOUNT_CREATING, "SYSTEM", "Bank account creation started");
        applications.save(application);
        return new ProvisioningApplication(application.getApplicationId(), application.getCustomerId(), application.getProductCode());
    }
    @Transactional
    public AccountProvisioningResponse complete(UUID applicationId, CoreBankingAccountResponse external) {
        AccountApplication application=find(applicationId);
        if (application.getStatus()!=ApplicationStatus.ACCOUNT_CREATING) throw new AccountCreationNotAllowedException(application.getStatus());
        if (accounts.existsByApplicationApplicationId(applicationId)) throw new BankAccountAlreadyExistsException();
        BankAccount account=new BankAccount(); account.setApplication(application); account.setExternalAccountId(external.accountId());
        account.setAccountNumber(external.accountNumber()); account.setStatus(external.status()); account.setOpenedAt(external.openedAt());
        accounts.save(account);
        workflow.transition(application, ApplicationStatus.COMPLETED, "SYSTEM", "Bank account created successfully");
        applications.save(application);
        return new AccountProvisioningResponse(applicationId, application.getStatus(), account.getAccountNumber(), account.getStatus(), account.getOpenedAt());
    }
    private AccountApplication find(UUID id) { return applications.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id)); }
    public record ProvisioningApplication(UUID applicationId, String customerId, String productCode) {}
}
