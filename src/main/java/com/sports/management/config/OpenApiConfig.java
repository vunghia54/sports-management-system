package com.sports.management.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sportsManagementOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("Sports Management System API")
                        .version("1.0.0")
                        .description(
                                "REST API for managing sports tournaments " +
                                        "and matches. Supports CRUD, validation, " +
                                        "search, filter, sorting, pagination " +
                                        "and partial updates using PATCH."
                        )
                        .contact(new Contact()
                                .name("Sports Management Development Team"))
                        .license(new License()
                                .name("Educational Project"))
                );
    }
}
