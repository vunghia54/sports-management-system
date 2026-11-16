package com.sports.management.gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "app.report.max-export-pages=2"
})
@AutoConfigureMockMvc
class SystemReportTruncationIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static HttpServer mockCluster;

    private static volatile int tournamentsTotalPages = 2;
    private static volatile boolean abnormalEmptyOnPage1 = false;

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void startMockServices() throws IOException {
        mockCluster = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        mockCluster.createContext("/api/v1/auth/me", ex ->
                writeJson(ex, 200, Map.of("username", "admin", "role", "ROLE_ADMIN", "permissions", List.of("reports.read_all"))));

        mockCluster.createContext("/api/v1/tournaments", ex -> {
            int page = parsePage(ex);
            if (abnormalEmptyOnPage1 && page == 1) {
                writeJson(ex, 200, Map.of(
                        "content", List.of(),
                        "page", 1, "size", 50, "totalElements", 5, "totalPages", tournamentsTotalPages
                ));
                return;
            }
            writeJson(ex, 200, Map.of(
                    "content", List.of(Map.of("id", page + 1, "name", "Giải trang " + page)),
                    "page", page, "size", 50, "totalElements", tournamentsTotalPages, "totalPages", tournamentsTotalPages
            ));
        });

        mockCluster.createContext("/api/v1/sports", ex -> writeJson(ex, 200, List.of()));
        mockCluster.createContext("/api/v1/competitions", ex -> writeJson(ex, 200, List.of()));
        mockCluster.createContext("/api/v1/teams/paged", ex -> writeJson(ex, 200, Map.of("content", List.of(), "page", 0, "totalElements", 0, "totalPages", 0)));
        mockCluster.createContext("/api/v1/athletes/paged", ex -> writeJson(ex, 200, Map.of("content", List.of(), "page", 0, "totalElements", 0, "totalPages", 0)));
        mockCluster.createContext("/api/v1/matches/paged", ex -> writeJson(ex, 200, Map.of("content", List.of(), "page", 0, "totalElements", 0, "totalPages", 0)));
        mockCluster.createContext("/api/v1/registrations/paged", ex -> writeJson(ex, 200, Map.of("content", List.of(), "page", 0, "totalElements", 0, "totalPages", 0)));
        mockCluster.createContext("/api/v1/results/paged", ex -> writeJson(ex, 200, Map.of("content", List.of(), "page", 0, "totalElements", 0, "totalPages", 0)));
        mockCluster.createContext("/api/v1/rankings/details", ex -> writeJson(ex, 200, List.of()));
        mockCluster.createContext("/api/v1/notifications/paged", ex -> writeJson(ex, 200, Map.of("content", List.of(), "page", 0, "totalElements", 0, "totalPages", 0)));
        mockCluster.createContext("/api/v1/auth/users/paged", ex -> writeJson(ex, 200, Map.of("content", List.of(), "page", 0, "totalElements", 0, "totalPages", 0)));

        mockCluster.start();
    }

    @AfterAll
    static void stopMockServices() {
        if (mockCluster != null) {
            mockCluster.stop(0);
        }
    }

    @DynamicPropertySource
    static void registerUrls(DynamicPropertyRegistry registry) {
        String base = "http://127.0.0.1:" + mockCluster.getAddress().getPort();
        registry.add("services.auth.url", () -> base);
        registry.add("services.tournament.url", () -> base);
        registry.add("services.team.url", () -> base);
        registry.add("services.athlete.url", () -> base);
        registry.add("services.match.url", () -> base);
        registry.add("services.result.url", () -> base);
        registry.add("services.ranking.url", () -> base);
        registry.add("services.notification.url", () -> base);
    }

    @BeforeEach
    void reset() {
        tournamentsTotalPages = 2;
        abnormalEmptyOnPage1 = false;
    }

    private static int parsePage(HttpExchange ex) {
        String q = ex.getRequestURI().getQuery();
        if (q == null) return 0;
        for (String pair : q.split("&")) {
            if (pair.startsWith("page=")) {
                return Integer.parseInt(pair.substring("page=".length()));
            }
        }
        return 0;
    }

    private static void writeJson(HttpExchange ex, int status, Object payload) throws IOException {
        byte[] bytes = JSON.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Test
    void exactMaxPageLimit_succeedsWithCompleteStatus() throws Exception {
        tournamentsTotalPages = 2; // exactly equals app.report.max-export-pages=2
        mockMvc.perform(get("/api/v1/reports/system-export")
                        .header("Authorization", "Bearer admin-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.partial").value(false))
                .andExpect(jsonPath("$.sourceStatuses.tournaments").value("OK"))
                .andExpect(jsonPath("$.data.tournaments", hasSize(2)));
    }

    @Test
    void exceedingMaxPageLimit_fails503ByDefaultAndMarksPartialWithReasonWhenAllowed() throws Exception {
        tournamentsTotalPages = 3; // exceeds app.report.max-export-pages=2 -> truncated!

        mockMvc.perform(get("/api/v1/reports/system-export")
                        .header("Authorization", "Bearer admin-jwt"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.exportStatus").value("FAILED"))
                .andExpect(jsonPath("$.sourceStatuses.tournaments").value("INCOMPLETE"))
                .andExpect(jsonPath("$.missingSources[0].source").value("tournaments"))
                .andExpect(jsonPath("$.missingSources[0].truncated").value(true))
                .andExpect(jsonPath("$.missingSources[0].reason", containsString("giới hạn")));

        mockMvc.perform(get("/api/v1/reports/system-export")
                        .param("allowPartial", "true")
                        .header("Authorization", "Bearer admin-jwt"))
                .andExpect(status().isPartialContent())
                .andExpect(jsonPath("$.exportStatus").value("PARTIAL"))
                .andExpect(jsonPath("$.partial").value(true))
                .andExpect(jsonPath("$.sourceStatuses.tournaments").value("INCOMPLETE"))
                .andExpect(jsonPath("$.missingSources[0].source").value("tournaments"))
                .andExpect(jsonPath("$.data.tournaments", hasSize(2)));
    }

    @Test
    void abnormalEmptyPageWhileMorePagesExist_fails503AndDoesNotReportComplete() throws Exception {
        tournamentsTotalPages = 3;
        abnormalEmptyOnPage1 = true;

        mockMvc.perform(get("/api/v1/reports/system-export")
                        .header("Authorization", "Bearer admin-jwt"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.exportStatus").value("FAILED"))
                .andExpect(jsonPath("$.sourceStatuses.tournaments").value("INCOMPLETE"))
                .andExpect(jsonPath("$.missingSources[0].source").value("tournaments"))
                .andExpect(jsonPath("$.missingSources[0].reason", containsString("rỗng bất thường")));
    }
}
