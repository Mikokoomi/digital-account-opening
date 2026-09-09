package com.digitalbank.accountopening.bankaccount;

import com.digitalbank.accountopening.application.*;
import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import com.digitalbank.accountopening.application.workflow.ApplicationWorkflowService;
import com.digitalbank.accountopening.common.exception.*;
import com.digitalbank.accountopening.integration.corebanking.*;
import com.digitalbank.accountopening.integration.tracking.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountProvisioningStateServiceTest {
    @Mock AccountApplicationRepository applications; @Mock BankAccountRepository accounts;
    @Mock IntegrationRequestRepository integrations; @Mock ApplicationStatusHistoryRepository history;
    Clock clock=Clock.fixed(Instant.parse("2026-09-09T02:00:00Z"),ZoneOffset.UTC);
    AccountProvisioningStateService service;
    @BeforeEach void setup(){service=new AccountProvisioningStateService(applications,accounts,integrations,new ApplicationWorkflowService(history),clock);}

    @Test void prepareInitialCreatesStableOperationAndMovesToAccountCreating(){
        AccountApplication a=application(ApplicationStatus.APPROVED); when(applications.findById(a.getApplicationId())).thenReturn(Optional.of(a));
        when(integrations.findByApplicationApplicationIdAndIntegrationType(a.getApplicationId(),IntegrationType.CORE_BANKING_CREATE_ACCOUNT)).thenReturn(Optional.empty());
        var result=service.prepareInitial(a.getApplicationId());
        assertFalse(result.alreadyCompleted()); assertEquals("CREATE_ACCOUNT:"+a.getApplicationId(),result.context().idempotencyKey());
        assertEquals(ApplicationStatus.ACCOUNT_CREATING,a.getStatus());
        verify(integrations).save(argThat(x->x.getAttemptCount()==0&&x.getStatus()==IntegrationStatus.IN_PROGRESS));
    }
    @Test void completedRequestReturnsExistingLocalAccountWithoutTransition(){
        AccountApplication a=application(ApplicationStatus.COMPLETED); BankAccount b=bankAccount(a);
        when(applications.findById(a.getApplicationId())).thenReturn(Optional.of(a)); when(accounts.findByApplicationApplicationId(a.getApplicationId())).thenReturn(Optional.of(b));
        var result=service.prepareInitial(a.getApplicationId()); assertTrue(result.alreadyCompleted()); assertEquals("ACC-1",result.completed().accountNumber());
        verifyNoInteractions(integrations,history);
    }
    @Test void accountCreatingRequestIsRejectedAsInProgress(){
        AccountApplication a=application(ApplicationStatus.ACCOUNT_CREATING); when(applications.findById(a.getApplicationId())).thenReturn(Optional.of(a));
        assertThrows(AccountCreationInProgressException.class,()->service.prepareInitial(a.getApplicationId()));
    }
    @Test void prepareRetryReusesOperationAndTransitionsBack(){
        AccountApplication a=application(ApplicationStatus.RETRY_PENDING); IntegrationRequest r=integration(a,IntegrationStatus.RETRY_PENDING);
        when(applications.findById(a.getApplicationId())).thenReturn(Optional.of(a));
        when(integrations.findByApplicationApplicationIdAndIntegrationType(a.getApplicationId(),IntegrationType.CORE_BANKING_CREATE_ACCOUNT)).thenReturn(Optional.of(r));
        var result=service.prepareRetry(a.getApplicationId()); assertEquals(r.getId(),result.integrationId()); assertEquals(r.getIdempotencyKey(),result.idempotencyKey());
        assertEquals(ApplicationStatus.ACCOUNT_CREATING,a.getStatus()); assertEquals(IntegrationStatus.IN_PROGRESS,r.getStatus());
    }
    @Test void beforeAttemptIsCumulative(){
        AccountApplication a=application(ApplicationStatus.ACCOUNT_CREATING); IntegrationRequest r=integration(a,IntegrationStatus.IN_PROGRESS); r.setAttemptCount(3);
        when(integrations.findById(r.getId())).thenReturn(Optional.of(r)); service.beforeAttempt(r.getId()); assertEquals(4,r.getAttemptCount());
    }
    @Test void exhaustedMovesApplicationAndOperationToRetryPending(){
        AccountApplication a=application(ApplicationStatus.ACCOUNT_CREATING); IntegrationRequest r=integration(a,IntegrationStatus.IN_PROGRESS);
        when(applications.findById(a.getApplicationId())).thenReturn(Optional.of(a)); when(integrations.findById(r.getId())).thenReturn(Optional.of(r));
        service.exhaust(a.getApplicationId(),r.getId()); assertEquals(ApplicationStatus.RETRY_PENDING,a.getStatus()); assertEquals(IntegrationStatus.RETRY_PENDING,r.getStatus());
    }
    @Test void completeStoresOneReferenceAndMarksSucceeded(){
        AccountApplication a=application(ApplicationStatus.ACCOUNT_CREATING); IntegrationRequest r=integration(a,IntegrationStatus.IN_PROGRESS);
        when(applications.findById(a.getApplicationId())).thenReturn(Optional.of(a)); when(integrations.findById(r.getId())).thenReturn(Optional.of(r));
        when(accounts.findByApplicationApplicationId(a.getApplicationId())).thenReturn(Optional.empty());
        var external=new CoreBankingAccountResponse(UUID.randomUUID(),"ACC-1",a.getApplicationId(),"CUS001","P","ACTIVE",OffsetDateTime.now(clock));
        var result=service.complete(a.getApplicationId(),r.getId(),external);
        assertEquals(ApplicationStatus.COMPLETED,result.applicationStatus()); assertEquals(IntegrationStatus.SUCCEEDED,r.getStatus());
        verify(accounts).save(argThat(x->x.getAccountNumber().equals("ACC-1")));
    }
    private AccountApplication application(ApplicationStatus status){AccountApplication a=new AccountApplication();a.setApplicationId(UUID.randomUUID());a.setCustomerId("CUS001");a.setProductCode("P");a.setStatus(status);return a;}
    private IntegrationRequest integration(AccountApplication a,IntegrationStatus status){IntegrationRequest r=new IntegrationRequest();r.setId(UUID.randomUUID());r.setApplication(a);r.setIntegrationType(IntegrationType.CORE_BANKING_CREATE_ACCOUNT);r.setIdempotencyKey("CREATE_ACCOUNT:"+a.getApplicationId());r.setStatus(status);r.setCreatedAt(OffsetDateTime.now(clock));r.setUpdatedAt(OffsetDateTime.now(clock));return r;}
    private BankAccount bankAccount(AccountApplication a){BankAccount b=new BankAccount();b.setApplication(a);b.setAccountNumber("ACC-1");b.setStatus("ACTIVE");b.setOpenedAt(OffsetDateTime.now(clock));return b;}
}
