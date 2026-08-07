package com.digitalbank.accountopening.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CifKycClientConfig {

    @Bean(name = "cifKycRestClient")
    RestClient cifKycRestClient(
            RestClient.Builder restClientBuilder,
            @Value("${integration.cif-kyc.base-url}") String baseUrl
    ) {
        return restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }
}
