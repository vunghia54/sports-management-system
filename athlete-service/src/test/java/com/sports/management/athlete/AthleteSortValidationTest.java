package com.sports.management.athlete;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AthleteSortValidationTest {

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:b2athletedb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sortByAndDirectionValidation_RejectsInvalidWith400AndAcceptsValid() throws Exception {
        mockMvc.perform(get("/api/v1/athletes?sortBy=nonExistentField"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/athletes/paged?sortBy=passwordHash"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/athletes/paged?sortBy=id&direction=random"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/athletes/paged?sortBy=fullName&direction=desc"))
                .andExpect(status().isOk());
    }
}
