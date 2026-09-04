package com.digitalbank.accountopening.bankaccount;

import com.digitalbank.accountopening.application.*;
import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import com.digitalbank.accountopening.application.workflow.ApplicationWorkflowService;
import com.digitalbank.accountopening.common.exception.*;
import com.digitalbank.accountopening.integration.corebanking.CoreBankingAccountResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountProvisioningStateServiceTest {
    @Mock AccountApplicationRepository applications; @Mock BankAccountRepository accounts; @Mock ApplicationStatusHistoryRepository history;
    AccountProvisioningStateService service;
    @BeforeEach void setup(){ service=new AccountProvisioningStateService(applications,accounts,new ApplicationWorkflowService(history)); }
    @Test void startMovesApprovedToAccountCreatingWithHistory(){
        AccountApplication a=application(ApplicationStatus.APPROVED); when(applications.findById(a.getApplicationId())).thenReturn(Optional.of(a));
        var result=service.start(a.getApplicationId()); assertEquals(ApplicationStatus.ACCOUNT_CREATING,a.getStatus()); assertEquals(a.getApplicationId(),result.applicationId());
        verify(history).save(argThat(h->h.getFromStatus()==ApplicationStatus.APPROVED&&h.getToStatus()==ApplicationStatus.ACCOUNT_CREATING));
    }
    @Test void startRejectsEveryNonApprovedStatus(){
        for(ApplicationStatus status:ApplicationStatus.values()) if(status!=ApplicationStatus.APPROVED){ AccountApplication a=application(status); when(applications.findById(a.getApplicationId())).thenReturn(Optional.of(a)); assertThrows(AccountCreationNotAllowedException.class,()->service.start(a.getApplicationId())); }
        verify(history,never()).save(any());
    }
    @Test void startRejectsLocalDuplicate(){
        AccountApplication a=application(ApplicationStatus.APPROVED); when(applications.findById(a.getApplicationId())).thenReturn(Optional.of(a)); when(accounts.existsByApplicationApplicationId(a.getApplicationId())).thenReturn(true);
        assertThrows(BankAccountAlreadyExistsException.class,()->service.start(a.getApplicationId()));
    }
    @Test void completeSavesReferenceAndMovesToCompleted(){
        AccountApplication a=application(ApplicationStatus.ACCOUNT_CREATING); when(applications.findById(a.getApplicationId())).thenReturn(Optional.of(a));
        OffsetDateTime opened=OffsetDateTime.now(); var ext=new CoreBankingAccountResponse(UUID.randomUUID(),"ACC-1",a.getApplicationId(),"CUS001","P","ACTIVE",opened);
        var result=service.complete(a.getApplicationId(),ext); assertEquals(ApplicationStatus.COMPLETED,result.applicationStatus());
        verify(accounts).save(argThat(x->x.getExternalAccountId().equals(ext.accountId())&&x.getAccountNumber().equals("ACC-1")));
        verify(history).save(argThat(h->h.getFromStatus()==ApplicationStatus.ACCOUNT_CREATING&&h.getToStatus()==ApplicationStatus.COMPLETED));
    }
    private AccountApplication application(ApplicationStatus status){ AccountApplication a=new AccountApplication(); a.setApplicationId(UUID.randomUUID()); a.setCustomerId("CUS001"); a.setProductCode("P"); a.setStatus(status); return a; }
}
