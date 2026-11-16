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
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false"
})
@AutoConfigureMockMvc
class SystemReportExportIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static HttpServer mockCluster;
    private static volatile boolean failRankingService = false;

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void startMockServices() throws IOException {
        mockCluster = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        mockCluster.createContext("/api/v1/auth/me", ex -> {
            String auth = ex.getRequestHeaders().getFirst("Authorization");
            if ("Bearer admin-jwt".equals(auth)) {
                writeJson(ex, 200, Map.of("username", "admin", "role", "ROLE_ADMIN", "permissions", List.of("reports.read_all")));
            } else if ("Bearer org-jwt".equals(auth)) {
                writeJson(ex, 200, Map.of("username", "organizer1", "role", "ROLE_ORGANIZER", "permissions", List.of("reports.read_own_tournaments")));
            } else {
                writeJson(ex, 401, Map.of("message", "Invalid token"));
            }
        });

        // Multi-page users response with secret keys that MUST be stripped by export sanitization
        mockCluster.createContext("/api/v1/auth/users/paged", ex -> {
            int page = parsePage(ex);
            if (page == 0) {
                writeJson(ex, 200, Map.of(
                        "content", List.of(Map.of("id", 1, "username", "admin", "password", "super-secret-hash", "role", "ROLE_ADMIN")),
                        "page", 0, "size", 50, "totalElements", 2, "totalPages", 2
                ));
            } else {
                writeJson(ex, 200, Map.of(
                        "content", List.of(Map.of("id", 2, "username", "org1", "token", "leaked-jwt-token", "role", "ROLE_ORGANIZER")),
                        "page", 1, "size", 50, "totalElements", 2, "totalPages", 2
                ));
            }
        });

        // Multi-page tournaments
        mockCluster.createContext("/api/v1/tournaments", ex -> {
            int page = parsePage(ex);
            if (page == 0) {
                writeJson(ex, 200, Map.of(
                        "content", List.of(Map.of("id", 1, "name", "Giải V.League 2026"), Map.of("id", 2, "name", "Cúp Quốc Gia")),
                        "page", 0, "size", 50, "totalElements", 3, "totalPages", 2
                ));
            } else {
                writeJson(ex, 200, Map.of(
                        "content", List.of(Map.of("id", 3, "name", "Siêu Cúp")),
                        "page", 1, "size", 50, "totalElements", 3, "totalPages", 2
                ));
            }
        });

        mockCluster.createContext("/api/v1/sports", ex -> writeJson(ex, 200, List.of(Map.of("id", 1, "name", "Bóng đá"))));
        mockCluster.createContext("/api/v1/competitions", ex -> writeJson(ex, 200, List.of(Map.of("id", 10, "name", "V.League Nam"))));
        mockCluster.createContext("/api/v1/teams/paged", ex -> writeJson(ex, 200, Map.of(
                "content", List.of(Map.of("id", 1, "name", "CLB Hà Nội")), "page", 0, "totalPages", 1)));
        mockCluster.createContext("/api/v1/athletes/paged", ex -> writeJson(ex, 200, Map.of(
                "content", List.of(Map.of("id", 1, "fullName", "Nguyễn Văn Quyết", "identityCardNumber", "001092004567")),
                "page", 0, "totalPages", 1)));
        mockCluster.createContext("/api/v1/matches/paged", ex -> writeJson(ex, 200, Map.of(
                "content", List.of(Map.of("id", 10, "participant1", "CLB Hà Nội", "participant2", "Nam Định")),
                "page", 0, "totalPages", 1)));
        mockCluster.createContext("/api/v1/registrations/paged", ex -> writeJson(ex, 200, Map.of(
                "content", List.of(Map.of("id", 1, "participantName", "CLB Hà Nội", "status", "APPROVED")),
                "page", 0, "totalPages", 1)));
        mockCluster.createContext("/api/v1/results/paged", ex -> writeJson(ex, 200, Map.of(
                "content", List.of(Map.of("id", 5, "matchId", 10, "scoreParticipant1", 2, "scoreParticipant2", 1, "status", "CONFIRMED")),
                "page", 0, "totalPages", 1)));
        mockCluster.createContext("/api/v1/rankings/details", ex -> {
            if (failRankingService) {
                writeJson(ex, 503, Map.of("status", 503, "message", "Ranking service down"));
                return;
            }
            writeJson(ex, 200, List.of(Map.of("ranking", Map.of("id", 1, "competitionId", 10), "items", List.of())));
        });
        mockCluster.createContext("/api/v1/notifications/paged", ex -> writeJson(ex, 200, Map.of(
                "content", List.of(Map.of("id", 1, "eventType", "MATCH_FINISHED", "message", "Done")),
                "page", 0, "totalPages", 1)));

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
        failRankingService = false;
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
    void exportEndpoint_enforcesBackendAdminAuthorization() throws Exception {
        mockMvc.perform(get("/api/v1/reports/system-export"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/reports/system-export")
                        .header("Authorization", "Bearer org-jwt"))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportEndpoint_aggregatesAllPagesSanitizesSecretsAndReportsTimestampAndCounts() throws Exception {
        mockMvc.perform(get("/api/v1/reports/system-export")
                        .header("Authorization", "Bearer admin-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.partial").value(false))
                .andExpect(jsonPath("$.exportedBy").value("admin"))
                .andExpect(jsonPath("$.generatedAt").isNotEmpty())
                .andExpect(jsonPath("$.summary.tournamentsCount").value(3))
                .andExpect(jsonPath("$.data.tournaments", hasSize(3)))
                .andExpect(jsonPath("$.summary.usersCount").value(2))
                .andExpect(jsonPath("$.data.users", hasSize(2)))
                .andExpect(jsonPath("$.data.athletes[0].identityCardNumber").value("********4567"))
                .andExpect(content().string(not(containsString("super-secret-hash"))))
                .andExpect(content().string(not(containsString("leaked-jwt-token"))));
    }

    @Test
    void exportEndpoint_fails503ByDefaultWhenUpstreamDownAndMarksPartialOnExplicitAllowPartial() throws Exception {
        failRankingService = true;

        mockMvc.perform(get("/api/v1/reports/system-export")
                        .header("Authorization", "Bearer admin-jwt"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.exportStatus").value("FAILED"))
                .andExpect(jsonPath("$.missingSources[0].source").value("rankings"));

        mockMvc.perform(get("/api/v1/reports/system-export")
                        .param("allowPartial", "true")
                        .header("Authorization", "Bearer admin-jwt"))
                .andExpect(status().isPartialContent())
                .andExpect(jsonPath("$.exportStatus").value("PARTIAL"))
                .andExpect(jsonPath("$.partial").value(true))
                .andExpect(jsonPath("$.missingSources[0].source").value("rankings"))
                .andExpect(jsonPath("$.sourceStatuses.rankings").value("FAILED"))
                .andExpect(jsonPath("$.sourceStatuses.tournaments").value("OK"))
                .andExpect(jsonPath("$.summary.failedSourcesCount").value(1));
    }
}
