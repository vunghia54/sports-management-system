package com.sports.management.gateway;

import com.sports.management.gateway.config.CorsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "app.cors.allowed-origins=http://localhost:8080,https://portal.sports.local"
})
@AutoConfigureMockMvc
class GatewayCorsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void cors_allowsConfiguredOriginAndRejectsDisallowedOrigin() throws Exception {
        // Allowed configured origin succeeds with exact origin echoed (not '*')
        mockMvc.perform(options("/api/v1/gateway/routes")
                        .header(HttpHeaders.ORIGIN, "http://localhost:8080")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:8080"));

        mockMvc.perform(get("/api/v1/gateway/routes")
                        .header(HttpHeaders.ORIGIN, "https://portal.sports.local"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://portal.sports.local"));

        // Arbitrary evil origin is rejected by Spring CORS without wildcard fallback
        mockMvc.perform(get("/api/v1/gateway/routes")
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void corsConfig_rejectsWildcardOriginConfiguration() {
        assertThrows(IllegalStateException.class, () -> new CorsConfig("*"));
        assertThrows(IllegalStateException.class, () -> new CorsConfig("http://localhost:8080, *"));
        assertThrows(IllegalStateException.class, () -> new CorsConfig("   "));
    }
}
