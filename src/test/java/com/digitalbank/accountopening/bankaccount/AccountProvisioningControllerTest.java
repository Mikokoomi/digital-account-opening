package com.digitalbank.accountopening.bankaccount;

import com.digitalbank.accountopening.application.enums.ApplicationStatus;
import com.digitalbank.accountopening.bankaccount.dto.AccountProvisioningResponse;
import com.digitalbank.accountopening.common.exception.GlobalExceptionHandler;
import com.digitalbank.accountopening.integration.corebanking.CoreBankingClientException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;
import com.digitalbank.accountopening.integration.tracking.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountProvisioningControllerTest {
    @Test void successReturnsResponseSchema() throws Exception {
        ApplicationAccountProvisioningService service=mock(ApplicationAccountProvisioningService.class); UUID id=UUID.randomUUID();
        when(service.createAccount(id)).thenReturn(new AccountProvisioningResponse(id,ApplicationStatus.COMPLETED,"ACC-1","ACTIVE",OffsetDateTime.now()));
        mvc(service).perform(post("/api/applications/{id}/create-account",id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationStatus").value("COMPLETED")).andExpect(jsonPath("$.data.accountNumber").value("ACC-1"));
    }
    @Test void technicalFailureReturns503() throws Exception {
        ApplicationAccountProvisioningService service=mock(ApplicationAccountProvisioningService.class); UUID id=UUID.randomUUID(); when(service.createAccount(id)).thenThrow(new CoreBankingClientException("down"));
        mvc(service).perform(post("/api/applications/{id}/create-account",id)).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.errorCode").value("CORE_BANKING_SERVICE_UNAVAILABLE"));
    }
    @Test void retryEndpointDelegates() throws Exception {
        ApplicationAccountProvisioningService service=mock(ApplicationAccountProvisioningService.class); UUID id=UUID.randomUUID();
        when(service.retryAccountCreation(id)).thenReturn(new AccountProvisioningResponse(id,ApplicationStatus.COMPLETED,"ACC-1","ACTIVE",OffsetDateTime.now()));
        mvc(service,mock(AccountProvisioningStateService.class)).perform(post("/api/applications/{id}/retry-account-creation",id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationStatus").value("COMPLETED"));
    }
    @Test void trackingEndpointIsReadOnly() throws Exception {
        ApplicationAccountProvisioningService service=mock(ApplicationAccountProvisioningService.class); AccountProvisioningStateService states=mock(AccountProvisioningStateService.class); UUID id=UUID.randomUUID();
        when(states.getIntegrationRequests(id)).thenReturn(List.of());
        mvc(service,states).perform(get("/api/applications/{id}/integration-requests",id)).andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
    }
    private MockMvc mvc(ApplicationAccountProvisioningService service){return mvc(service,mock(AccountProvisioningStateService.class));}
    private MockMvc mvc(ApplicationAccountProvisioningService service,AccountProvisioningStateService states){return MockMvcBuilders.standaloneSetup(new AccountProvisioningController(service,states)).setControllerAdvice(new GlobalExceptionHandler()).build();}
}
