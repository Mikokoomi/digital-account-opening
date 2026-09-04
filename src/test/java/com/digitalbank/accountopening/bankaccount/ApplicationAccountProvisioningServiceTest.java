package com.digitalbank.accountopening.bankaccount;

import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import com.digitalbank.accountopening.bankaccount.dto.AccountProvisioningResponse;
import com.digitalbank.accountopening.integration.corebanking.*;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicationAccountProvisioningServiceTest {
    @Test void successRunsStartHttpAndCompletionInOrder() {
        AccountProvisioningStateService states=mock(AccountProvisioningStateService.class);
        CoreBankingClient client=mock(CoreBankingClient.class);
        UUID id=UUID.randomUUID(); OffsetDateTime opened=OffsetDateTime.now();
        var snapshot=new AccountProvisioningStateService.ProvisioningApplication(id,"CUS001","CURRENT_ACCOUNT");
        var external=new CoreBankingAccountResponse(UUID.randomUUID(),"ACC-1",id,"CUS001","CURRENT_ACCOUNT","ACTIVE",opened);
        var expected=new AccountProvisioningResponse(id,ApplicationStatus.COMPLETED,"ACC-1","ACTIVE",opened);
        when(states.start(id)).thenReturn(snapshot); when(client.createAccount(any())).thenReturn(external); when(states.complete(id,external)).thenReturn(expected);
        assertEquals(expected,new ApplicationAccountProvisioningService(states,client).createAccount(id));
        var order=inOrder(states,client); order.verify(states).start(id); order.verify(client).createAccount(any()); order.verify(states).complete(id,external);
    }
    @Test void technicalFailureLeavesCompletionUncalled() {
        AccountProvisioningStateService states=mock(AccountProvisioningStateService.class); CoreBankingClient client=mock(CoreBankingClient.class);
        UUID id=UUID.randomUUID(); when(states.start(id)).thenReturn(new AccountProvisioningStateService.ProvisioningApplication(id,"CUS001","P"));
        when(client.createAccount(any())).thenThrow(new CoreBankingClientException("down"));
        assertThrows(CoreBankingClientException.class,()->new ApplicationAccountProvisioningService(states,client).createAccount(id));
        verify(states,never()).complete(any(),any());
    }
}
