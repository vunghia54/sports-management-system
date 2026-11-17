package com.sports.management.result;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:result_timeout_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "jwt.secret=SportsManagementSystemSuperSecretKeyWithAtLeast256BitsLongForHMACSHA256!",
        "app.result.sync-enabled=false",
        "services.http.connect-timeout-ms=500",
        "services.http.read-timeout-ms=350"
})
@AutoConfigureMockMvc
class ResultTimeoutAndRequestIdTest {

    private static final String JWT_SECRET = "SportsManagementSystemSuperSecretKeyWithAtLeast256BitsLongForHMACSHA256!";
    private static HttpServer slowMatchServer;
    private static ExecutorService executor;
    private static final List<String> capturedMatchRequestIds = new CopyOnWriteArrayList<>();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void registerSlowProvider(DynamicPropertyRegistry registry) throws IOException {
        if (slowMatchServer == null) {
            executor = Executors.newCachedThreadPool();
            slowMatchServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            slowMatchServer.setExecutor(executor);
            slowMatchServer.createContext("/api/v1/matches/", exchange -> {
                String reqId = exchange.getRequestHeaders().getFirst("X-Request-Id");
                if (reqId != null) {
                    capturedMatchRequestIds.add(reqId);
                }
                try {
                    Thread.sleep(2200);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                byte[] body = "{\"id\":99,\"status\":\"ONGOING\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                try {
                    exchange.sendResponseHeaders(200, body.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(body);
                    }
                } catch (IOException ignored) {
                    // Client closed socket after read timeout expired
                }
            });
            slowMatchServer.start();
        }
        int port = slowMatchServer.getAddress().getPort();
        registry.add("services.match.url", () -> "http://127.0.0.1:" + port);
        registry.add("services.ranking.url", () -> "http://127.0.0.1:" + port);
        registry.add("services.tournament.url", () -> "http://127.0.0.1:" + port);
    }

    @AfterAll
    static void stopServer() {
        if (slowMatchServer != null) {
            slowMatchServer.stop(0);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @BeforeEach
    void clearCaptured() {
        capturedMatchRequestIds.clear();
    }

    @Test
    void slowProviderExceedingReadTimeout_returns503WithinBoundedTimeAndPropagatesXRequestId() throws Exception {
        String refereeToken = issueToken("referee1", "ROLE_REFEREE");
        String requestId = "timeout-trace-2026";

        long startMs = System.currentTimeMillis();
        mockMvc.perform(post("/api/v1/results")
                        .header("Authorization", refereeToken)
                        .header("X-Request-Id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":99,\"scoreParticipant1\":2,\"scoreParticipant2\":1}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("X-Request-Id", requestId));
        long elapsedMs = System.currentTimeMillis() - startMs;

        assertTrue(elapsedMs < 1800,
                "Read timeout (350ms) must fail fast with 503 well before slow server 2200ms sleep; elapsedMs=" + elapsedMs);
        assertEquals(List.of(requestId), capturedMatchRequestIds,
                "RestClientConfig interceptor must propagate MDC X-Request-Id to downstream match-service");
    }

    private static String issueToken(String username, String role) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(username)
                .claim("role", role)
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 3600_000L))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(JWT_SECRET.getBytes(StandardCharsets.UTF_8)));
        return "Bearer " + jwt.serialize();
    }
}
