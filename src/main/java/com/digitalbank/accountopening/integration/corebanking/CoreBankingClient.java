package com.digitalbank.accountopening.integration.corebanking;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
public class CoreBankingClient {
    private final RestClient restClient;
    public CoreBankingClient(@Qualifier("coreBankingRestClient") RestClient restClient) { this.restClient = restClient; }
    public CoreBankingAccountResponse createAccount(String idempotencyKey, CoreBankingCreateAccountRequest request) {
        try {
            CoreBankingAccountResponse response = restClient.post().uri("/api/accounts")
                    .header("Idempotency-Key", idempotencyKey)
                    .body(request).retrieve().body(CoreBankingAccountResponse.class);
            validate(response, request);
            return response;
        } catch (HttpClientErrorException.Conflict exception) {
            throw new CoreBankingIdempotencyConflictException(exception);
        } catch (RestClientResponseException exception) {
            int status=exception.getStatusCode().value();
            if (status == 429 || status >= 500) throw new CoreBankingRetryableException("Core Banking returned HTTP " + status, status, exception);
            throw new CoreBankingNonRetryableException("Core Banking rejected the request with HTTP " + status, status, exception);
        } catch (RestClientException exception) {
            throw new CoreBankingRetryableException("Core Banking service is unavailable", null, exception);
        }
    }
    private void validate(CoreBankingAccountResponse response, CoreBankingCreateAccountRequest request) {
        if (response == null || response.accountId() == null || response.accountNumber() == null
                || response.accountNumber().isBlank() || response.applicationId() == null
                || !response.applicationId().equals(request.applicationId())
                || response.status() == null || response.status().isBlank() || response.openedAt() == null) {
            throw new CoreBankingNonRetryableException("Core Banking returned an invalid response", null);
        }
    }
}
