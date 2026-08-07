package com.digitalbank.accountopening.integration.cifkyc;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;

@Component
public class CifKycClient {

    private final RestClient restClient;

    public CifKycClient(@Qualifier("cifKycRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public Optional<CifKycCustomerResponse> getCustomer(String customerId) {
        try {
            CifKycCustomerResponse customer = restClient.get()
                    .uri("/api/customers/{customerId}", customerId)
                    .retrieve()
                    .body(CifKycCustomerResponse.class);

            if (customer == null) {
                throw new CifKycClientException("CIF/KYC service returned an empty response");
            }

            return Optional.of(customer);
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        } catch (RestClientResponseException exception) {
            throw new CifKycClientException(
                    "CIF/KYC service returned HTTP " + exception.getStatusCode().value(),
                    exception
            );
        } catch (RestClientException exception) {
            throw new CifKycClientException("CIF/KYC service is unavailable", exception);
        }
    }
}
