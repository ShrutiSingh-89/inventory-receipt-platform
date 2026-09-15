package com.portfolio.inventory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI inventoryReceiptOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Inventory Receipt API")
                .version("1.0.0")
                .description("REST API for searching items and creating inventory receipts."));
    }
}

