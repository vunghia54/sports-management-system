package com.sports.management.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false"
})
@AutoConfigureMockMvc
class DemoModeDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void demoConfig_disabledByDefaultAndContainsNoAccounts() throws Exception {
        mockMvc.perform(get("/api/v1/demo/config"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.accounts", hasSize(0)))
                .andExpect(content().string(not(containsString("admin123"))));
    }

    @Test
    void demoScript_returns404WhenDisabledEvenWithQueryParam() throws Exception {
        mockMvc.perform(get("/js/demo-mode.js"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(content().string(not(containsString("admin123"))));

        mockMvc.perform(get("/js/demo-mode.js").param("demo", "1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("admin123"))));
    }

    @Test
    void staticDemoModeScriptDoesNotExistInPublicStaticAssets() {
        assertFalse(new ClassPathResource("static/js/demo-mode.js").exists(),
                "Static public static/js/demo-mode.js must be removed so credentials are never exposed");
    }
}
