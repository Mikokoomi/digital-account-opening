package com.digitalbank.accountopening.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI accountOpeningOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Digital Account Opening API")
                .version("v1")
                .description("API for the digital account opening and approval workflow."));
    }
}
