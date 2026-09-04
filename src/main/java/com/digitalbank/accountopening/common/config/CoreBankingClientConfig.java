package com.digitalbank.accountopening.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.web.client.RestClient;

@Configuration
public class CoreBankingClientConfig {
    @Bean(name = "coreBankingRestClient")
    RestClient coreBankingRestClient(RestClient.Builder builder,
            @Value("${integration.core-banking.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
