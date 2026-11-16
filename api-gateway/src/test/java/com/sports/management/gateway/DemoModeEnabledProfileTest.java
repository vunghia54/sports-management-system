package com.sports.management.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
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
@ActiveProfiles("demo")
@AutoConfigureMockMvc
class DemoModeEnabledProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void demoProfile_servesDemoConfigAndScriptWithNoStoreCacheHeader() throws Exception {
        mockMvc.perform(get("/api/v1/demo/config"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.accounts", hasSize(greaterThanOrEqualTo(5))));

        mockMvc.perform(get("/js/demo-mode.js"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(content().string(containsString("window.__VN_DEMO_MODE__")))
                .andExpect(content().string(containsString("organizer1")));
    }
}
