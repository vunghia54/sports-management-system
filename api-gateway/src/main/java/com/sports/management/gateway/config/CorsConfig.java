package com.sports.management.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins:http://localhost:8080}") String rawOrigins) {
        List<String> parsed = Arrays.stream((rawOrigins != null ? rawOrigins : "").split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        if (parsed.isEmpty()) {
            throw new IllegalStateException("app.cors.allowed-origins must contain at least one explicit origin");
        }
        if (parsed.stream().anyMatch("*"::equals)) {
            throw new IllegalStateException("Wildcard CORS origin '*' is not permitted");
        }
        this.allowedOrigins = parsed;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders(
                        "X-Instance-Id",
                        "X-Instance-Port",
                        "X-Gateway-Routed-Service",
                        "X-Tournament-Provider-Instance",
                        "X-Athlete-Provider-Instance"
                )
                .maxAge(3600);
    }
}
