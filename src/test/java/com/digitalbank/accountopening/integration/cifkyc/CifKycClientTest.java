package com.digitalbank.accountopening.integration.cifkyc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CifKycClientTest {

    private static final String BASE_URL = "http://cif-kyc.test";

    private MockRestServiceServer mockServer;
    private CifKycClient cifKycClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        cifKycClient = new CifKycClient(restClientBuilder.build());
    }

    @AfterEach
    void verifyRequests() {
        mockServer.verify();
    }

    @Test
    void getCustomer_shouldDeserializeCustomerWhenServiceReturnsOk() {
        mockServer.expect(once(), requestTo(BASE_URL + "/api/customers/CUS001"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "customerId": "CUS001",
                          "fullName": "Nguyen Van A",
                          "dateOfBirth": "1998-05-15",
                          "customerStatus": "ACTIVE",
                          "kycStatus": "VERIFIED",
                          "kycExpiryDate": "2027-12-31"
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<CifKycCustomerResponse> result = cifKycClient.getCustomer("CUS001");

        assertTrue(result.isPresent());
        CifKycCustomerResponse customer = result.orElseThrow();
        assertEquals("CUS001", customer.customerId());
        assertEquals("Nguyen Van A", customer.fullName());
        assertEquals(LocalDate.of(1998, 5, 15), customer.dateOfBirth());
        assertEquals("ACTIVE", customer.customerStatus());
        assertEquals("VERIFIED", customer.kycStatus());
        assertEquals(LocalDate.of(2027, 12, 31), customer.kycExpiryDate());
    }

    @Test
    void getCustomer_shouldReturnEmptyWhenServiceReturnsNotFound() {
        mockServer.expect(once(), requestTo(BASE_URL + "/api/customers/CUS999"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        Optional<CifKycCustomerResponse> result = cifKycClient.getCustomer("CUS999");

        assertFalse(result.isPresent());
    }

    @Test
    void getCustomer_shouldThrowClientExceptionWhenServiceReturnsServerError() {
        mockServer.expect(once(), requestTo(BASE_URL + "/api/customers/CUS001"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        CifKycClientException exception = assertThrows(
                CifKycClientException.class,
                () -> cifKycClient.getCustomer("CUS001")
        );

        assertEquals("CIF/KYC service returned HTTP 500", exception.getMessage());
    }

    @Test
    void getCustomer_shouldThrowClientExceptionWhenServiceIsUnavailable() {
        mockServer.expect(once(), requestTo(BASE_URL + "/api/customers/CUS001"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> {
                    throw new IOException("Connection refused");
                });

        CifKycClientException exception = assertThrows(
                CifKycClientException.class,
                () -> cifKycClient.getCustomer("CUS001")
        );

        assertEquals("CIF/KYC service is unavailable", exception.getMessage());
    }
}
