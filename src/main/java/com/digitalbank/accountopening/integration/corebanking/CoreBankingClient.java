package com.digitalbank.accountopening.integration.corebanking;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
public class CoreBankingClient {
    private final RestClient restClient;
    public CoreBankingClient(@Qualifier("coreBankingRestClient") RestClient restClient) { this.restClient = restClient; }
    public CoreBankingAccountResponse createAccount(CoreBankingCreateAccountRequest request) {
        try {
            CoreBankingAccountResponse response = restClient.post().uri("/api/accounts")
                    .body(request).retrieve().body(CoreBankingAccountResponse.class);
            validate(response, request);
            return response;
        } catch (HttpClientErrorException.Conflict exception) {
            throw new CoreBankingDuplicateApplicationException();
        } catch (RestClientResponseException exception) {
            throw new CoreBankingClientException("Core Banking returned HTTP " + exception.getStatusCode().value(), exception);
        } catch (RestClientException exception) {
            throw new CoreBankingClientException("Core Banking service is unavailable", exception);
        }
    }
    private void validate(CoreBankingAccountResponse response, CoreBankingCreateAccountRequest request) {
        if (response == null || response.accountId() == null || response.accountNumber() == null
                || response.accountNumber().isBlank() || response.applicationId() == null
                || !response.applicationId().equals(request.applicationId())
                || response.status() == null || response.status().isBlank() || response.openedAt() == null) {
            throw new CoreBankingClientException("Core Banking returned an invalid response");
        }
    }
}
