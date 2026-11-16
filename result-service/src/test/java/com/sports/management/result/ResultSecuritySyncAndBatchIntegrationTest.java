package com.sports.management.result;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sports.management.result.model.Result;
import com.sports.management.result.repository.ResultDetailRepository;
import com.sports.management.result.repository.ResultRepository;
import com.sports.management.result.service.ResultSyncRecoveryWorker;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:result_sec_sync_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "jwt.secret=SportsManagementSystemSuperSecretKeyWithAtLeast256BitsLongForHMACSHA256!",
        "app.result.sync-enabled=false"
})
@AutoConfigureMockMvc
class ResultSecuritySyncAndBatchIntegrationTest {

    private static final String JWT_SECRET = "SportsManagementSystemSuperSecretKeyWithAtLeast256BitsLongForHMACSHA256!";
    private static final ObjectMapper JSON = new ObjectMapper();

    private static HttpServer matchServer;
    private static HttpServer rankingServer;
    private static HttpServer tournamentServer;

    private static volatile int matchGetStatus = 200;
    private static volatile int matchPatchStatus = 200;
    private static volatile int rankingPostStatus = 200;
    private static volatile int tournamentGetStatus = 200;

    private static final AtomicInteger matchPatchCalls = new AtomicInteger();
    private static final AtomicInteger rankingPostCalls = new AtomicInteger();
    private static final List<String> capturedMatchPatchAuthHeaders = new CopyOnWriteArrayList<>();
    private static final List<String> capturedRankingPostAuthHeaders = new CopyOnWriteArrayList<>();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private ResultDetailRepository resultDetailRepository;

    @Autowired
    private ResultSyncRecoveryWorker syncRecoveryWorker;

    @BeforeAll
    static void startMockUpstreams() throws IOException {
        matchServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        matchServer.createContext("/api/v1/matches/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String idStr = path.substring("/api/v1/matches/".length());
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                if (matchGetStatus != 200) {
                    writeJson(exchange, matchGetStatus, Map.of("message", "Match upstream error"));
                    return;
                }
                if ("2".equals(idStr)) {
                    writeJson(exchange, 200, Map.of(
                            "id", 2,
                            "tournamentId", 200,
                            "competitionId", 20,
                            "participant1", "Viettel",
                            "participant2", "CAHN",
                            "referee", "referee2", // intentionally matches referee2 display name while username is referee1
                            "refereeUsername", "referee1",
                            "status", "ONGOING"
                    ));
                } else {
                    writeJson(exchange, 200, Map.of(
                            "id", Long.parseLong(idStr),
                            "tournamentId", 100,
                            "competitionId", 10,
                            "participant1", "Hà Nội",
                            "participant2", "Nam Định",
                            "referee", "Trọng Tài Quốc Gia",
                            "refereeUsername", "referee1",
                            "status", "ONGOING"
                    ));
                }
                return;
            }
            if ("PATCH".equalsIgnoreCase(exchange.getRequestMethod())) {
                matchPatchCalls.incrementAndGet();
                String auth = exchange.getRequestHeaders().getFirst("Authorization");
                capturedMatchPatchAuthHeaders.add(auth != null ? auth : "");
                if (matchPatchStatus != 200) {
                    writeJson(exchange, matchPatchStatus, Map.of("status", matchPatchStatus, "message", "Match patch failed"));
                    return;
                }
                writeJson(exchange, 200, Map.of("id", Long.parseLong(idStr), "status", "FINISHED"));
                return;
            }
            writeJson(exchange, 405, Map.of("message", "Method not allowed"));
        });
        matchServer.start();

        rankingServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        rankingServer.createContext("/api/v1/rankings/competition/", exchange -> {
            rankingPostCalls.incrementAndGet();
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            capturedRankingPostAuthHeaders.add(auth != null ? auth : "");
            if (rankingPostStatus != 200) {
                writeJson(exchange, rankingPostStatus, Map.of("status", rankingPostStatus, "message", "Ranking error"));
                return;
            }
            writeJson(exchange, 200, Map.of("ranking", Map.of("competitionId", 10)));
        });
        rankingServer.start();

        tournamentServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        tournamentServer.createContext("/api/v1/tournaments/", exchange -> {
            if (tournamentGetStatus != 200) {
                writeJson(exchange, tournamentGetStatus, Map.of("message", "Tournament unavailable"));
                return;
            }
            String path = exchange.getRequestURI().getPath();
            String idStr = path.substring("/api/v1/tournaments/".length());
            String owner = "200".equals(idStr) ? "organizer2" : "organizer1";
            writeJson(exchange, 200, Map.of("id", Long.parseLong(idStr), "name", "Giải #" + idStr, "organizerUsername", owner));
        });
        tournamentServer.start();
    }

    @AfterAll
    static void stopMockUpstreams() {
        if (matchServer != null) matchServer.stop(0);
        if (rankingServer != null) rankingServer.stop(0);
        if (tournamentServer != null) tournamentServer.stop(0);
    }

    @DynamicPropertySource
    static void registerDynamicUrls(DynamicPropertyRegistry registry) {
        registry.add("services.match.url", () -> "http://127.0.0.1:" + matchServer.getAddress().getPort());
        registry.add("services.ranking.url", () -> "http://127.0.0.1:" + rankingServer.getAddress().getPort());
        registry.add("services.tournament.url", () -> "http://127.0.0.1:" + tournamentServer.getAddress().getPort());
    }

    @BeforeEach
    void resetState() {
        resultDetailRepository.deleteAll();
        resultRepository.deleteAll();
        matchGetStatus = 200;
        matchPatchStatus = 200;
        rankingPostStatus = 200;
        tournamentGetStatus = 200;
        matchPatchCalls.set(0);
        rankingPostCalls.set(0);
        capturedMatchPatchAuthHeaders.clear();
        capturedRankingPostAuthHeaders.clear();
    }

    private static void writeJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        byte[] bytes = JSON.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String issueToken(String username, String role) throws Exception {
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

    @Test
    void authorizationChecks_enforceMatchAndTournamentOwnershipAndBlockPatchBypass() throws Exception {
        String viewerToken = issueToken("user1", "ROLE_VIEWER");
        String ref1Token = issueToken("referee1", "ROLE_REFEREE");
        String ref2Token = issueToken("referee2", "ROLE_REFEREE");
        String org1Token = issueToken("organizer1", "ROLE_ORGANIZER");
        String org2Token = issueToken("organizer2", "ROLE_ORGANIZER");
        String adminToken = issueToken("admin", "ROLE_ADMIN");

        // 1. Unauthenticated and Viewer rejected
        mockMvc.perform(post("/api/v1/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":1,\"scoreParticipant1\":2,\"scoreParticipant2\":1}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/results")
                        .header("Authorization", viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":1,\"scoreParticipant1\":2,\"scoreParticipant2\":1}"))
                .andExpect(status().isForbidden());
        assertEquals(0L, resultRepository.count());

        // 2. Unassigned referee2 denied even though match #2 display referee=="referee2" (strict refereeUsername="referee1")
        mockMvc.perform(post("/api/v1/results")
                        .header("Authorization", ref2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":2,\"scoreParticipant1\":1,\"scoreParticipant2\":0}"))
                .andExpect(status().isForbidden());
        assertEquals(0L, resultRepository.count());

        // 3. Assigned referee1 records result for match #1 -> CREATED PENDING_CONFIRMATION
        String createdJson = mockMvc.perform(post("/api/v1/results")
                        .header("Authorization", ref1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":1,\"scoreParticipant1\":3,\"scoreParticipant2\":1,\"notes\":\"Ghi nhận bởi TT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.status").value("PENDING_CONFIRMATION"))
                .andReturn().getResponse().getContentAsString();
        Long resultId = Long.valueOf(JSON.readTree(createdJson).path("result").path("id").asLong());

        // 4. Assigned referee1 CANNOT confirm via PUT confirm OR bypass via PATCH status=CONFIRMED
        mockMvc.perform(put("/api/v1/results/" + resultId + "/confirm")
                        .header("Authorization", ref1Token))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/results/" + resultId)
                        .header("Authorization", ref1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isForbidden());

        assertEquals("PENDING_CONFIRMATION", resultRepository.findById(resultId).orElseThrow().getStatus());

        // Assigned referee CAN patch notes
        mockMvc.perform(patch("/api/v1/results/" + resultId)
                        .header("Authorization", ref1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Cập nhật ghi chú\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Cập nhật ghi chú"));

        // 5. Foreign organizer (organizer2) CANNOT edit or confirm match #1 (tournament 100 owned by organizer1)
        mockMvc.perform(post("/api/v1/results")
                        .header("Authorization", org2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":1,\"scoreParticipant1\":9,\"scoreParticipant2\":9}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/results/" + resultId + "/confirm")
                        .header("Authorization", org2Token))
                .andExpect(status().isForbidden());
        assertEquals(3, resultRepository.findById(resultId).orElseThrow().getScoreParticipant1());

        // 6. When upstream verification (tournament-service) returns 503, Organizer is blocked & DB stays untouched
        tournamentGetStatus = 503;
        mockMvc.perform(put("/api/v1/results/" + resultId + "/confirm")
                        .header("Authorization", org1Token))
                .andExpect(status().isServiceUnavailable());
        assertEquals("PENDING_CONFIRMATION", resultRepository.findById(resultId).orElseThrow().getStatus());
        tournamentGetStatus = 200;

        // 7. Owning organizer (organizer1) confirms and spoofed confirmedBy payload is ignored
        capturedMatchPatchAuthHeaders.clear();
        mockMvc.perform(put("/api/v1/results/" + resultId + "/confirm")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmedBy\":\"spoofedAdmin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.result.confirmedBy").value("organizer1"))
                .andExpect(jsonPath("$.result.syncStatus").value("SYNCED"));
        assertFalse(capturedMatchPatchAuthHeaders.isEmpty());
        SignedJWT syncJwt = SignedJWT.parse(capturedMatchPatchAuthHeaders.get(0).replace("Bearer ", ""));
        assertEquals("ROLE_INTERNAL_RESULT_SYNC", syncJwt.getJWTClaimsSet().getClaim("role"),
                "Synchronous markMatchFinished during confirm MUST use internal ROLE_INTERNAL_RESULT_SYNC token, not caller JWT");

        // 8. Admin can record & confirm on tournament 200 cleanly
        String m2Json = mockMvc.perform(post("/api/v1/results")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":2,\"scoreParticipant1\":2,\"scoreParticipant2\":2}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long r2Id = Long.valueOf(JSON.readTree(m2Json).path("result").path("id").asLong());
        mockMvc.perform(put("/api/v1/results/" + r2Id + "/confirm")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.confirmedBy").value("admin"))
                .andExpect(jsonPath("$.result.syncStatus").value("SYNCED"));
    }

    @Test
    void syncStates_distinguishMatchErrorRankingErrorIdempotentRetries403AndCrashRecovery() throws Exception {
        String org1Token = issueToken("organizer1", "ROLE_ORGANIZER");

        // Record result for match 10
        String body = mockMvc.perform(post("/api/v1/results")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":10,\"scoreParticipant1\":2,\"scoreParticipant2\":0}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long r10Id = Long.valueOf(JSON.readTree(body).path("result").path("id").asLong());

        // Case A: Match Service fails (503) during confirm -> returns 202 Accepted + syncStatus=FAILED, Ranking NOT called
        matchPatchStatus = 503;
        mockMvc.perform(put("/api/v1/results/" + r10Id + "/confirm")
                        .header("Authorization", org1Token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.result.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.result.syncStatus").value("FAILED"))
                .andExpect(jsonPath("$.result.matchSynced").value(false))
                .andExpect(jsonPath("$.result.rankingSynced").value(false))
                .andExpect(jsonPath("$.result.syncPermanentFailure").value(false));
        assertTrue(matchPatchCalls.get() >= 1);
        assertEquals(0, rankingPostCalls.get());

        // Case B: Match succeeds (200), then Ranking Service fails (503) during retry
        matchPatchCalls.set(0);
        rankingPostCalls.set(0);
        matchPatchStatus = 200;
        rankingPostStatus = 503;
        mockMvc.perform(post("/api/v1/results/" + r10Id + "/sync")
                        .header("Authorization", org1Token))
                .andExpect(status().isServiceUnavailable());

        Result afterMatchOkRankingFail = resultRepository.findById(r10Id).orElseThrow();
        assertTrue(afterMatchOkRankingFail.isMatchSynced(), "Match step must persist true even when ranking fails");
        assertFalse(afterMatchOkRankingFail.isRankingSynced());
        assertEquals("FAILED", afterMatchOkRankingFail.getSyncStatus());
        assertFalse(afterMatchOkRankingFail.isSyncPermanentFailure());
        assertEquals(1, matchPatchCalls.get());
        assertTrue(rankingPostCalls.get() >= 1);

        // Case C: Ranking recovers (200) -> manual retry skips Match PATCH (already synced!) and completes Ranking
        matchPatchCalls.set(0);
        rankingPostCalls.set(0);
        rankingPostStatus = 200;
        mockMvc.perform(post("/api/v1/results/" + r10Id + "/sync")
                        .header("Authorization", org1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.syncStatus").value("SYNCED"))
                .andExpect(jsonPath("$.result.matchSynced").value(true))
                .andExpect(jsonPath("$.result.rankingSynced").value(true));
        assertEquals(0, matchPatchCalls.get(), "Match PATCH should not be duplicated when already matchSynced=true");
        assertEquals(1, rankingPostCalls.get());

        // Case D: Calling sync again is idempotent (no additional Match or Ranking calls)
        mockMvc.perform(post("/api/v1/results/" + r10Id + "/sync")
                        .header("Authorization", org1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.syncStatus").value("SYNCED"));
        assertEquals(0, matchPatchCalls.get());
        assertEquals(1, rankingPostCalls.get());

        // Case E: Downstream 403 Permission Denied sets syncPermanentFailure=true so auto-worker does NOT loop endlessly
        String body11 = mockMvc.perform(post("/api/v1/results")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":11,\"scoreParticipant1\":1,\"scoreParticipant2\":1}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long r11Id = Long.valueOf(JSON.readTree(body11).path("result").path("id").asLong());

        matchPatchStatus = 403;
        mockMvc.perform(put("/api/v1/results/" + r11Id + "/confirm")
                        .header("Authorization", org1Token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.result.syncStatus").value("FAILED"))
                .andExpect(jsonPath("$.result.syncPermanentFailure").value(true));

        Result permFail = resultRepository.findById(r11Id).orElseThrow();
        assertNull(permFail.getNextRetryAt());
        int callsBeforeWorker = matchPatchCalls.get();
        int processedPerm = syncRecoveryWorker.processPendingAndRetryableJobs();
        assertEquals(0, processedPerm, "Auto-retry worker must NOT retry permanent 403 failures");
        assertEquals(callsBeforeWorker, matchPatchCalls.get());

        // Case F: Interrupted PENDING sync job on startup/restart is recovered by worker using ROLE_INTERNAL_RESULT_SYNC
        matchPatchStatus = 200;
        rankingPostStatus = 200;
        Result crashedJob = new Result();
        crashedJob.setMatchId(12L);
        crashedJob.setCompetitionId(10L);
        crashedJob.setScoreParticipant1(4);
        crashedJob.setScoreParticipant2(2);
        crashedJob.setWinner("Hà Nội");
        crashedJob.setStatus("CONFIRMED");
        crashedJob.setConfirmedBy("organizer1");
        crashedJob.setSyncStatus("PENDING");
        crashedJob.setMatchSynced(false);
        crashedJob.setRankingSynced(false);
        crashedJob = resultRepository.saveAndFlush(crashedJob);

        capturedMatchPatchAuthHeaders.clear();
        capturedRankingPostAuthHeaders.clear();
        int recovered = syncRecoveryWorker.processPendingAndRetryableJobs();
        assertEquals(1, recovered);

        Result afterRecovery = resultRepository.findById(crashedJob.getId()).orElseThrow();
        assertEquals("SYNCED", afterRecovery.getSyncStatus());
        assertTrue(afterRecovery.isMatchSynced());
        assertTrue(afterRecovery.isRankingSynced());

        // Verify worker minted short-lived ROLE_INTERNAL_RESULT_SYNC JWT (NOT user JWT, NOT ROLE_ADMIN)
        assertFalse(capturedMatchPatchAuthHeaders.isEmpty());
        String workerBearer = capturedMatchPatchAuthHeaders.get(0).replace("Bearer ", "");
        SignedJWT parsed = SignedJWT.parse(workerBearer);
        assertEquals("ROLE_INTERNAL_RESULT_SYNC", parsed.getJWTClaimsSet().getClaim("role"));
        assertEquals("internal:result-sync", parsed.getJWTClaimsSet().getClaim("scope"));
        assertNotNull(parsed.getJWTClaimsSet().getExpirationTime());
    }

    @Test
    void byMatchesEndpoint_returnsExactResultsEvenWithOver100TotalSystemResults() throws Exception {
        List<Result> batch = new ArrayList<>();
        // Seed 120 results across matches 1..120; matches 105..120 sit outside page 0 of /paged?size=100
        for (long mId = 1; mId <= 120; mId++) {
            Result r = new Result();
            r.setMatchId(mId);
            r.setCompetitionId(mId <= 104 ? 1L : 3L);
            r.setScoreParticipant1((int) (mId % 5));
            r.setScoreParticipant2(1);
            r.setStatus(mId % 2 == 0 ? "CONFIRMED" : "PENDING_CONFIRMATION");
            r.setSyncStatus("CONFIRMED".equals(r.getStatus()) ? "SYNCED" : "NOT_REQUIRED");
            batch.add(r);
        }
        resultRepository.saveAllAndFlush(batch);

        List<Long> targetMatchIds = List.of(108L, 115L, 120L, 999L); // 999 has no result yet
        String queryParam = targetMatchIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        mockMvc.perform(get("/api/v1/results/by-matches").param("matchIds", queryParam))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.matchId == 108)]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.matchId == 115)]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.matchId == 120)]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.matchId == 1)]").isEmpty());
    }
}
