package com.digitalbank.accountopening.bankaccount;

import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import com.digitalbank.accountopening.bankaccount.dto.AccountProvisioningResponse;
import com.digitalbank.accountopening.integration.corebanking.*;
import org.junit.jupiter.api.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApplicationAccountProvisioningServiceTest {
    AccountProvisioningStateService states; CoreBankingClient client; UUID applicationId; UUID integrationId;
    AccountProvisioningStateService.ProvisioningContext context; CoreBankingAccountResponse external; AccountProvisioningResponse completed;
    @BeforeEach void setup(){
        states=mock(AccountProvisioningStateService.class);client=mock(CoreBankingClient.class);applicationId=UUID.randomUUID();integrationId=UUID.randomUUID();
        context=new AccountProvisioningStateService.ProvisioningContext(applicationId,"CUS001","P",integrationId,"CREATE_ACCOUNT:"+applicationId);
        external=new CoreBankingAccountResponse(UUID.randomUUID(),"ACC-1",applicationId,"CUS001","P","ACTIVE",OffsetDateTime.now());
        completed=new AccountProvisioningResponse(applicationId,ApplicationStatus.COMPLETED,"ACC-1","ACTIVE",external.openedAt());
        when(states.prepareInitial(applicationId)).thenReturn(AccountProvisioningStateService.Preparation.pending(context));
        when(states.complete(applicationId,integrationId,external)).thenReturn(completed);
    }
    @Test void firstAttemptSuccessCompletes(){
        when(client.createAccount(context.idempotencyKey(),new CoreBankingCreateAccountRequest(applicationId,"CUS001","P"))).thenReturn(external);
        assertEquals(completed,service(3).createAccount(applicationId)); verify(states).beforeAttempt(integrationId); verify(states).complete(applicationId,integrationId,external);
    }
    @Test void technicalFailureThenSuccessUsesSameKey(){
        when(client.createAccount(eq(context.idempotencyKey()),any())).thenThrow(retryable()).thenReturn(external);
        assertEquals(completed,service(3).createAccount(applicationId)); verify(client,times(2)).createAccount(eq(context.idempotencyKey()),any());
        verify(states,times(2)).beforeAttempt(integrationId); verify(states).recordRetryableFailure(eq(integrationId),any());
    }
    @Test void twoFailuresThenThirdSuccess(){
        when(client.createAccount(eq(context.idempotencyKey()),any())).thenThrow(retryable()).thenThrow(retryable()).thenReturn(external);
        service(3).createAccount(applicationId); verify(client,times(3)).createAccount(eq(context.idempotencyKey()),any()); verify(states,times(3)).beforeAttempt(integrationId);
    }
    @Test void exhaustedRetriesMoveToRetryPending(){
        when(client.createAccount(anyString(),any())).thenThrow(retryable());
        assertThrows(CoreBankingRetryableException.class,()->service(3).createAccount(applicationId));
        verify(client,times(3)).createAccount(eq(context.idempotencyKey()),any()); verify(states).exhaust(applicationId,integrationId); verify(states,never()).complete(any(),any(),any());
    }
    @Test void nonRetryableFailureIsNotRetried(){
        var failure=new CoreBankingNonRetryableException("bad response",null); when(client.createAccount(anyString(),any())).thenThrow(failure);
        assertThrows(CoreBankingNonRetryableException.class,()->service(3).createAccount(applicationId));
        verify(client).createAccount(anyString(),any()); verify(states).fail(integrationId,failure);
    }
    @Test void completedReplayDoesNotCallCoreBanking(){
        when(states.prepareInitial(applicationId)).thenReturn(AccountProvisioningStateService.Preparation.completed(completed));
        assertEquals(completed,service(3).createAccount(applicationId)); verifyNoInteractions(client);
    }
    @Test void manualRetryUsesExistingContextAndCumulativeTracking(){
        when(states.prepareRetry(applicationId)).thenReturn(context); when(client.createAccount(eq(context.idempotencyKey()),any())).thenReturn(external);
        assertEquals(completed,service(3).retryAccountCreation(applicationId)); verify(states).prepareRetry(applicationId); verify(states).beforeAttempt(integrationId);
    }
    private ApplicationAccountProvisioningService service(int attempts){return new ApplicationAccountProvisioningService(states,client,attempts,0);}
    private CoreBankingRetryableException retryable(){return new CoreBankingRetryableException("timeout",null,new RuntimeException());}
}
