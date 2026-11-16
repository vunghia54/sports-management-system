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
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:result_comp_conc_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class ResultCompetitionAndConcurrencyTest {

    private static final String JWT_SECRET = "SportsManagementSystemSuperSecretKeyWithAtLeast256BitsLongForHMACSHA256!";
    private static final ObjectMapper JSON = new ObjectMapper();

    private static HttpServer matchServer;
    private static HttpServer rankingServer;
    private static HttpServer tournamentServer;
    private static ExecutorService httpExecutor;

    private static volatile boolean matchHasCompetition = true;
    private static volatile int matchGetStatus = 200;
    private static volatile int matchPatchStatus = 200;
    private static volatile int rankingPostStatus = 200;

    private static final AtomicInteger matchPatchCalls = new AtomicInteger();
    private static final AtomicInteger rankingPostCalls = new AtomicInteger();

    private static volatile CountDownLatch matchEnteredLatch;
    private static volatile CountDownLatch matchReleaseLatch;
    private static volatile CountDownLatch rankingEnteredLatch;
    private static volatile CountDownLatch rankingReleaseLatch;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private ResultDetailRepository resultDetailRepository;

    @Autowired
    private ResultSyncRecoveryWorker syncRecoveryWorker;

    @BeforeAll
    static void startMockServers() throws IOException {
        httpExecutor = Executors.newCachedThreadPool();
        matchServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        matchServer.setExecutor(httpExecutor);
        matchServer.createContext("/api/v1/matches/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            long id = Long.parseLong(path.substring("/api/v1/matches/".length()));
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                if (matchGetStatus != 200) {
                    writeJson(exchange, matchGetStatus, Map.of("message", "Match unavailable"));
                    return;
                }
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("id", id);
                body.put("tournamentId", 100);
                body.put("competitionId", matchHasCompetition ? 10 : null);
                body.put("participant1", "Hà Nội");
                body.put("participant2", "Nam Định");
                body.put("refereeUsername", "referee1");
                body.put("status", "ONGOING");
                writeJson(exchange, 200, body);
                return;
            }
            if ("PATCH".equalsIgnoreCase(exchange.getRequestMethod())) {
                matchPatchCalls.incrementAndGet();
                CountDownLatch entered = matchEnteredLatch;
                CountDownLatch release = matchReleaseLatch;
                if (entered != null) entered.countDown();
                if (release != null) {
                    try {
                        release.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {
                    }
                }
                if (matchPatchStatus != 200) {
                    writeJson(exchange, matchPatchStatus, Map.of("status", matchPatchStatus, "message", "Match patch failed"));
                    return;
                }
                writeJson(exchange, 200, Map.of("id", id, "status", "FINISHED"));
            }
        });
        matchServer.start();

        rankingServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        rankingServer.setExecutor(httpExecutor);
        rankingServer.createContext("/api/v1/rankings/competition/", exchange -> {
            rankingPostCalls.incrementAndGet();
            CountDownLatch entered = rankingEnteredLatch;
            CountDownLatch release = rankingReleaseLatch;
            if (entered != null) entered.countDown();
            if (release != null) {
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                }
            }
            if (rankingPostStatus != 200) {
                writeJson(exchange, rankingPostStatus, Map.of("status", rankingPostStatus, "message", "Ranking error"));
                return;
            }
            writeJson(exchange, 200, Map.of("ranking", Map.of("competitionId", 10)));
        });
        rankingServer.start();

        tournamentServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        tournamentServer.setExecutor(httpExecutor);
        tournamentServer.createContext("/api/v1/tournaments/", exchange ->
                writeJson(exchange, 200, Map.of("id", 100, "name", "Giải 100", "organizerUsername", "organizer1")));
        tournamentServer.start();
    }

    @AfterAll
    static void stopMockServers() {
        if (matchServer != null) matchServer.stop(0);
        if (rankingServer != null) rankingServer.stop(0);
        if (tournamentServer != null) tournamentServer.stop(0);
        if (httpExecutor != null) httpExecutor.shutdownNow();
    }

    @DynamicPropertySource
    static void registerDynamicUrls(DynamicPropertyRegistry registry) {
        registry.add("services.match.url", () -> "http://127.0.0.1:" + matchServer.getAddress().getPort());
        registry.add("services.ranking.url", () -> "http://127.0.0.1:" + rankingServer.getAddress().getPort());
        registry.add("services.tournament.url", () -> "http://127.0.0.1:" + tournamentServer.getAddress().getPort());
    }

    @BeforeEach
    void reset() {
        resultDetailRepository.deleteAll();
        resultRepository.deleteAll();
        matchHasCompetition = true;
        matchGetStatus = 200;
        matchPatchStatus = 200;
        rankingPostStatus = 200;
        matchPatchCalls.set(0);
        rankingPostCalls.set(0);
        matchEnteredLatch = null;
        matchReleaseLatch = null;
        rankingEnteredLatch = null;
        rankingReleaseLatch = null;
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
    void issue1_competitionIdRequired_rejectsConfirmAndRefusesFalseSyncedOnLegacyOrMissingMatchData() throws Exception {
        String orgToken = issueToken("organizer1", "ROLE_ORGANIZER");

        // 1. Match lacks competitionId -> record draft works, but confirm rejects 400 cleanly before persisting CONFIRMED
        matchHasCompetition = false;
        String draftJson = mockMvc.perform(post("/api/v1/results")
                        .header("Authorization", orgToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":50,\"scoreParticipant1\":1,\"scoreParticipant2\":0}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long draftId = Long.valueOf(JSON.readTree(draftJson).path("result").path("id").asLong());

        mockMvc.perform(put("/api/v1/results/" + draftId + "/confirm")
                        .header("Authorization", orgToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("competitionId")));

        Result unchanged = resultRepository.findById(draftId).orElseThrow();
        assertEquals("PENDING_CONFIRMATION", unchanged.getStatus());
        assertFalse(unchanged.isRankingSynced());

        // 2. Legacy/pending DB job missing competitionId + Match query returns 503 -> FAILED transient (NEVER SYNCED!)
        Result legacyJob = new Result();
        legacyJob.setMatchId(51L);
        legacyJob.setCompetitionId(null);
        legacyJob.setScoreParticipant1(2);
        legacyJob.setScoreParticipant2(1);
        legacyJob.setWinner("Hà Nội");
        legacyJob.setStatus("CONFIRMED");
        legacyJob.setConfirmedBy("organizer1");
        legacyJob.setSyncStatus("PENDING");
        legacyJob.setResultVersion(1L);
        legacyJob = resultRepository.saveAndFlush(legacyJob);

        matchGetStatus = 503;
        syncRecoveryWorker.processPendingAndRetryableJobs();
        Result afterMatchQueryFail = resultRepository.findById(legacyJob.getId()).orElseThrow();
        assertEquals("FAILED", afterMatchQueryFail.getSyncStatus());
        assertFalse(afterMatchQueryFail.isRankingSynced());
        assertFalse(afterMatchQueryFail.isSyncPermanentFailure(), "Transient match lookup 503 should allow retry");
        assertEquals(0, rankingPostCalls.get());

        // 3. Match lookup returns 200 but competitionId STILL null -> permanent FAILED (NEVER marks rankingSynced=true!)
        matchGetStatus = 200;
        matchHasCompetition = false;
        afterMatchQueryFail.setNextRetryAt(null);
        resultRepository.saveAndFlush(afterMatchQueryFail);

        syncRecoveryWorker.processPendingAndRetryableJobs();
        Result afterStillNullComp = resultRepository.findById(legacyJob.getId()).orElseThrow();
        assertEquals("FAILED", afterStillNullComp.getSyncStatus());
        assertFalse(afterStillNullComp.isRankingSynced());
        assertTrue(afterStillNullComp.isSyncPermanentFailure());
        assertTrue(afterStillNullComp.getSyncError().contains("competitionId"));
        assertEquals(0, rankingPostCalls.get());

        // 4. Match gets competitionId=10 -> manual retry resolves competitionId, calls Ranking, and marks SYNCED
        matchHasCompetition = true;
        mockMvc.perform(post("/api/v1/results/" + legacyJob.getId() + "/sync")
                        .header("Authorization", orgToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.syncStatus").value("SYNCED"))
                .andExpect(jsonPath("$.result.rankingSynced").value(true))
                .andExpect(jsonPath("$.result.competitionId").value(10));
        assertEquals(1, rankingPostCalls.get());
    }

    @Test
    void issue2_scoreEditedWhileWaitingOnMatchService_staleJobSuccessOrFailureNeverOverwritesNewVersion() throws Exception {
        String orgToken = issueToken("organizer1", "ROLE_ORGANIZER");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Variant A: Stale sync succeeds after score was edited concurrently
            String rJson = mockMvc.perform(post("/api/v1/results")
                            .header("Authorization", orgToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"matchId\":60,\"scoreParticipant1\":3,\"scoreParticipant2\":1}"))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            Long resId = Long.valueOf(JSON.readTree(rJson).path("result").path("id").asLong());

            matchEnteredLatch = new CountDownLatch(1);
            matchReleaseLatch = new CountDownLatch(1);

            Future<MvcResult> confirmFuture = executor.submit(() ->
                    mockMvc.perform(put("/api/v1/results/" + resId + "/confirm")
                                    .header("Authorization", orgToken))
                            .andReturn()
            );

            assertTrue(matchEnteredLatch.await(5, TimeUnit.SECONDS), "Sync job did not reach Match Service");

            // While sync job is blocked in Match Service, user edits score
            mockMvc.perform(post("/api/v1/results")
                            .header("Authorization", orgToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"matchId\":60,\"scoreParticipant1\":0,\"scoreParticipant2\":0}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.result.status").value("PENDING_CONFIRMATION"));

            // Release Match Service (returns 200 OK for stale confirm thread)
            matchReleaseLatch.countDown();
            confirmFuture.get(5, TimeUnit.SECONDS);

            // Crucial: Old job must NOT mark new score as SYNCED, CONFIRMED, matchSynced=true or call Ranking!
            Result afterStaleSuccess = resultRepository.findById(resId).orElseThrow();
            assertEquals("PENDING_CONFIRMATION", afterStaleSuccess.getStatus());
            assertEquals("NOT_REQUIRED", afterStaleSuccess.getSyncStatus());
            assertFalse(afterStaleSuccess.isMatchSynced());
            assertFalse(afterStaleSuccess.isRankingSynced());
            assertEquals(0, afterStaleSuccess.getScoreParticipant1());
            assertEquals(0, rankingPostCalls.get(), "Superseded version must abort before calling Ranking");

            // Variant B: Re-confirm edited score, edit again while Match FAILS with 503 -> stale failure must NOT mark new edit FAILED!
            matchPatchCalls.set(0);
            matchPatchStatus = 503;
            matchEnteredLatch = new CountDownLatch(1);
            matchReleaseLatch = new CountDownLatch(1);

            Future<MvcResult> confirmFailFuture = executor.submit(() ->
                    mockMvc.perform(put("/api/v1/results/" + resId + "/confirm")
                                    .header("Authorization", orgToken))
                            .andReturn()
            );

            assertTrue(matchEnteredLatch.await(5, TimeUnit.SECONDS));

            mockMvc.perform(post("/api/v1/results")
                            .header("Authorization", orgToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"matchId\":60,\"scoreParticipant1\":2,\"scoreParticipant2\":2}"))
                    .andExpect(status().isCreated());

            matchReleaseLatch.countDown();
            confirmFailFuture.get(5, TimeUnit.SECONDS);

            Result afterStaleFailure = resultRepository.findById(resId).orElseThrow();
            assertEquals("PENDING_CONFIRMATION", afterStaleFailure.getStatus());
            assertEquals("NOT_REQUIRED", afterStaleFailure.getSyncStatus());
            assertEquals(2, afterStaleFailure.getScoreParticipant1());
            assertEquals(null, afterStaleFailure.getSyncError(), "Old job 503 must NOT pollute new version with syncError");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void issue2_scoreEditedWhileWaitingOnRankingService_thenReconfirmRunsFullPipelineAndRecalculatesRanking() throws Exception {
        String orgToken = issueToken("organizer1", "ROLE_ORGANIZER");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            String rJson = mockMvc.perform(post("/api/v1/results")
                            .header("Authorization", orgToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"matchId\":70,\"scoreParticipant1\":4,\"scoreParticipant2\":0}"))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            Long resId = Long.valueOf(JSON.readTree(rJson).path("result").path("id").asLong());

            rankingEnteredLatch = new CountDownLatch(1);
            rankingReleaseLatch = new CountDownLatch(1);

            Future<MvcResult> confirmFuture = executor.submit(() ->
                    mockMvc.perform(put("/api/v1/results/" + resId + "/confirm")
                                    .header("Authorization", orgToken))
                            .andReturn()
            );

            assertTrue(rankingEnteredLatch.await(5, TimeUnit.SECONDS), "Sync job did not reach Ranking Service");

            // While Ranking Service is slow/in-flight, user edits score -> 1-2
            mockMvc.perform(post("/api/v1/results")
                            .header("Authorization", orgToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"matchId\":70,\"scoreParticipant1\":1,\"scoreParticipant2\":2}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.result.status").value("PENDING_CONFIRMATION"));

            // Also test old task failing after edit doesn't touch new version
            rankingPostStatus = 503;
            rankingReleaseLatch.countDown();
            confirmFuture.get(5, TimeUnit.SECONDS);

            Result afterOldFail = resultRepository.findById(resId).orElseThrow();
            assertEquals("PENDING_CONFIRMATION", afterOldFail.getStatus());
            assertEquals("NOT_REQUIRED", afterOldFail.getSyncStatus());
            assertFalse(afterOldFail.isMatchSynced(), "New edited version must reset matchSynced=false");
            assertFalse(afterOldFail.isRankingSynced(), "New edited version must reset rankingSynced=false");

            // Now confirm the NEW version (1-2): MUST execute BOTH Match PATCH and Ranking recalculate freshly!
            rankingEnteredLatch = null;
            rankingReleaseLatch = null;
            rankingPostStatus = 200;
            matchPatchCalls.set(0);
            rankingPostCalls.set(0);

            mockMvc.perform(put("/api/v1/results/" + resId + "/confirm")
                            .header("Authorization", orgToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.result.syncStatus").value("SYNCED"))
                    .andExpect(jsonPath("$.result.matchSynced").value(true))
                    .andExpect(jsonPath("$.result.rankingSynced").value(true))
                    .andExpect(jsonPath("$.result.scoreParticipant1").value(1))
                    .andExpect(jsonPath("$.result.scoreParticipant2").value(2));

            assertEquals(1, matchPatchCalls.get(), "Re-confirming edited version must invoke Match Service fresh");
            assertEquals(1, rankingPostCalls.get(), "Re-confirming edited version must recalculate Ranking fresh");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void issue2_twoConcurrentRetries_onlyOneExecutesDownstreamAndStateStaysConsistent() throws Exception {
        String orgToken = issueToken("organizer1", "ROLE_ORGANIZER");

        // Prepare a CONFIRMED + FAILED result with matchSynced=false, rankingSynced=false
        Result retryable = new Result();
        retryable.setMatchId(80L);
        retryable.setCompetitionId(10L);
        retryable.setScoreParticipant1(2);
        retryable.setScoreParticipant2(1);
        retryable.setWinner("Hà Nội");
        retryable.setStatus("CONFIRMED");
        retryable.setConfirmedBy("organizer1");
        retryable.setSyncStatus("FAILED");
        retryable.setResultVersion(2L);
        retryable = resultRepository.saveAndFlush(retryable);
        final Long retryId = retryable.getId();

        rankingEnteredLatch = new CountDownLatch(1);
        rankingReleaseLatch = new CountDownLatch(1);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> f1 = pool.submit(() ->
                    mockMvc.perform(post("/api/v1/results/" + retryId + "/sync")
                                    .header("Authorization", orgToken))
                            .andReturn()
            );

            assertTrue(rankingEnteredLatch.await(5, TimeUnit.SECONDS), "First retry must claim lease and hit Ranking Service");

            // Fire second concurrent retry while first retry is holding DB lease inside Ranking Service
            Future<MvcResult> f2 = pool.submit(() ->
                    mockMvc.perform(post("/api/v1/results/" + retryId + "/sync")
                                    .header("Authorization", orgToken))
                            .andReturn()
            );

            MvcResult r2 = f2.get(5, TimeUnit.SECONDS);
            assertEquals(202, r2.getResponse().getStatus(), "Concurrent retry must not duplicate calls; returns 202 In Progress");

            rankingReleaseLatch.countDown();
            MvcResult r1 = f1.get(5, TimeUnit.SECONDS);
            assertEquals(200, r1.getResponse().getStatus(), "Winning retry completes with 200 OK");

            assertEquals(1, matchPatchCalls.get(), "Match Service must be called exactly once");
            assertEquals(1, rankingPostCalls.get(), "Ranking Service must be called exactly once");

            Result finalRow = resultRepository.findById(retryId).orElseThrow();
            assertEquals("SYNCED", finalRow.getSyncStatus());
            assertTrue(finalRow.isMatchSynced());
            assertTrue(finalRow.isRankingSynced());
            assertEquals(1, finalRow.getSyncAttempts());
        } finally {
            pool.shutdownNow();
        }
    }
}
