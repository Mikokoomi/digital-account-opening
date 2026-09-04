package com.digitalbank.accountopening.integration.corebanking;

import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CoreBankingClientTest {
    MockRestServiceServer server; CoreBankingClient client; UUID id;
    @BeforeEach void setup(){ RestClient.Builder b=RestClient.builder().baseUrl("http://core.test"); server=MockRestServiceServer.bindTo(b).build(); client=new CoreBankingClient(b.build()); id=UUID.randomUUID(); }
    @AfterEach void verify(){server.verify();}
    @Test void createsAccount(){ expect(withSuccess(json(id),MediaType.APPLICATION_JSON)); assertEquals("ACC-1",client.createAccount(request()).accountNumber()); }
    @Test void invalidResponseIsTechnicalFailure(){ expect(withSuccess("{}",MediaType.APPLICATION_JSON)); assertThrows(CoreBankingClientException.class,()->client.createAccount(request())); }
    @Test void serverErrorIsTechnicalFailure(){ expect(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)); assertThrows(CoreBankingClientException.class,()->client.createAccount(request())); }
    @Test void conflictIsBusinessConflict(){ expect(withStatus(HttpStatus.CONFLICT)); assertThrows(CoreBankingDuplicateApplicationException.class,()->client.createAccount(request())); }
    private void expect(org.springframework.test.web.client.ResponseCreator response){ server.expect(once(),requestTo("http://core.test/api/accounts")).andExpect(method(HttpMethod.POST)).andRespond(response); }
    private CoreBankingCreateAccountRequest request(){return new CoreBankingCreateAccountRequest(id,"CUS001","P");}
    private String json(UUID applicationId){return "{\"accountId\":\""+UUID.randomUUID()+"\",\"accountNumber\":\"ACC-1\",\"applicationId\":\""+applicationId+"\",\"customerId\":\"CUS001\",\"productCode\":\"P\",\"status\":\"ACTIVE\",\"openedAt\":\""+OffsetDateTime.now()+"\"}";}
}
