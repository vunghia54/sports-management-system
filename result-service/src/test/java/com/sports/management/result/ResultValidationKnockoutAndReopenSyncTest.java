package com.sports.management.result;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sports.management.result.model.RankingSyncTask;
import com.sports.management.result.model.Result;
import com.sports.management.result.repository.RankingSyncTaskRepository;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:result_a6_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class ResultValidationKnockoutAndReopenSyncTest {

    private static final String JWT_SECRET = "SportsManagementSystemSuperSecretKeyWithAtLeast256BitsLongForHMACSHA256!";
    private static final ObjectMapper JSON = new ObjectMapper();

    private static HttpServer matchServer;
    private static HttpServer rankingServer;
    private static HttpServer tournamentServer;

    private static volatile int rankingPostStatus = 200;
    private static final AtomicInteger rankingPostCalls = new AtomicInteger();
    private static volatile List<Map<String, Object>> knockoutCompetitionMatches = List.of();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private ResultDetailRepository resultDetailRepository;

    @Autowired
    private RankingSyncTaskRepository rankingSyncTaskRepository;

    @Autowired
    private ResultSyncRecoveryWorker syncRecoveryWorker;

    @BeforeAll
    static void startMockServers() throws IOException {
        matchServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        matchServer.createContext("/api/v1/matches/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            long id = Long.parseLong(path.substring("/api/v1/matches/".length()));
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("id", id);
                body.put("tournamentId", 100);
                body.put("refereeUsername", "referee1");
                body.put("participant1", "Hà Nội");
                body.put("participant2", "Viettel");
                if (id == 1L) {
                    body.put("competitionId", 10);
                    body.put("status", "SCHEDULED");
                    body.put("matchTime", LocalDateTime.now().minusHours(1).toString());
                } else if (id == 2L) {
                    body.put("competitionId", 10);
                    body.put("status", "CANCELLED");
                    body.put("matchTime", LocalDateTime.now().minusHours(1).toString());
                } else if (id == 3L) {
                    body.put("competitionId", 10);
                    body.put("status", "POSTPONED");
                    body.put("matchTime", LocalDateTime.now().minusHours(1).toString());
                } else if (id == 4L) {
                    body.put("competitionId", 10);
                    body.put("status", "ONGOING");
                    body.put("matchTime", LocalDateTime.now().plusHours(4).toString());
                } else if (id == 5L) {
                    // Knockout competition 20 match
                    body.put("competitionId", 20);
                    body.put("status", "ONGOING");
                    body.put("matchTime", LocalDateTime.now().minusMinutes(30).toString());
                } else {
                    // Normal Round-Robin ONGOING match
                    body.put("competitionId", 10);
                    body.put("status", "ONGOING");
                    body.put("matchTime", LocalDateTime.now().minusMinutes(30).toString());
                }
                writeJson(exchange, 200, body);
                return;
            }
            if ("PATCH".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 200, Map.of("id", id, "status", "FINISHED"));
            }
        });
        // Danh sach tran theo noi dung thi dau (dung de kiem tra vong sau cua KNOCKOUT)
        matchServer.createContext("/api/v1/matches/competition/", exchange ->
                writeJson(exchange, 200, knockoutCompetitionMatches));
        matchServer.start();

        rankingServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        rankingServer.createContext("/api/v1/rankings/competition/", exchange -> {
            rankingPostCalls.incrementAndGet();
            if (rankingPostStatus != 200) {
                writeJson(exchange, rankingPostStatus, Map.of("status", rankingPostStatus, "message", "Ranking temp failure"));
                return;
            }
            writeJson(exchange, 200, Map.of("ranking", Map.of("competitionId", 10)));
        });
        rankingServer.start();

        tournamentServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        tournamentServer.createContext("/api/v1/tournaments/", exchange ->
                writeJson(exchange, 200, Map.of("id", 100, "organizerUsername", "organizer1")));
        tournamentServer.createContext("/api/v1/competitions/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/api/v1/competitions/20/format")) {
                writeJson(exchange, 200, Map.of("competitionId", 20, "formatType", "KNOCKOUT"));
            } else {
                writeJson(exchange, 200, Map.of("competitionId", 10, "formatType", "ROUND_ROBIN"));
            }
        });
        tournamentServer.start();
    }

    @AfterAll
    static void stopMockServers() {
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
        rankingSyncTaskRepository.deleteAll();
        resultDetailRepository.deleteAll();
        resultRepository.deleteAll();
        rankingPostStatus = 200;
        rankingPostCalls.set(0);
        knockoutCompetitionMatches = List.of();
    }

    private static void writeJson(HttpExchange exchange, int code, Object payload) throws IOException {
        byte[] bytes = JSON.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
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
    void recordResult_rejectsScheduledCancelledPostponedAndFutureMatch_andClearsEmptyDetails() throws Exception {
        String refToken = issueToken("referee1", "ROLE_REFEREE");

        // SCHEDULED (1), CANCELLED (2), POSTPONED (3), Future ONGOING (4) -> all 400
        for (long badMatchId : List.of(1L, 2L, 3L, 4L)) {
            mockMvc.perform(post("/api/v1/results")
                            .header("Authorization", refToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"matchId\":" + badMatchId + ",\"scoreParticipant1\":1,\"scoreParticipant2\":0}"))
                    .andExpect(status().isBadRequest());
        }

        // Valid match 10 with 1 detail
        mockMvc.perform(post("/api/v1/results")
                        .header("Authorization", refToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":10,\"scoreParticipant1\":2,\"scoreParticipant2\":1,"
                                + "\"details\":[{\"detailType\":\"GOAL\",\"participantName\":\"Hà Nội\",\"athleteName\":\"Hùng Dũng\",\"detailTime\":\"10'\"}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.details", hasSize(1)));

        // Re-record match 10 with empty details [] -> clears old details!
        mockMvc.perform(post("/api/v1/results")
                        .header("Authorization", refToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":10,\"scoreParticipant1\":3,\"scoreParticipant2\":1,\"details\":[]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.details", hasSize(0)));
        assertTrue(resultDetailRepository.findAll().isEmpty());
    }

    @Test
    void refereeCannotOverwriteConfirmedResult_reopenAndDeleteTriggerRetryableRankingRecalc() throws Exception {
        String refToken = issueToken("referee1", "ROLE_REFEREE");
        String orgToken = issueToken("organizer1", "ROLE_ORGANIZER");
        String adminToken = issueToken("admin", "ROLE_ADMIN");

        // Referee records and Organizer confirms match 10 (Round-Robin comp 10)
        String body = mockMvc.perform(post("/api/v1/results")
                        .header("Authorization", refToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":10,\"scoreParticipant1\":2,\"scoreParticipant2\":0}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long resId = JSON.readTree(body).get("result").get("id").asLong();

        mockMvc.perform(put("/api/v1/results/" + resId + "/confirm")
                        .header("Authorization", orgToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.rankingSynced").value(true));

        // Referee CANNOT overwrite CONFIRMED result or PATCH notes on it -> 403
        mockMvc.perform(post("/api/v1/results")
                        .header("Authorization", refToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":10,\"scoreParticipant1\":0,\"scoreParticipant2\":0}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/results/" + resId)
                        .header("Authorization", refToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Hack\"}"))
                .andExpect(status().isForbidden());

        // Owning organizer reopens via PATCH status=PENDING_CONFIRMATION while Ranking Service is temporarily 503 -> task saved & retried by worker!
        rankingPostCalls.set(0);
        rankingPostStatus = 503;
        mockMvc.perform(patch("/api/v1/results/" + resId)
                        .header("Authorization", orgToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING_CONFIRMATION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_CONFIRMATION"));

        assertTrue(rankingPostCalls.get() >= 1);
        List<RankingSyncTask> tasks = rankingSyncTaskRepository.findByCompetitionIdOrderByIdAsc(10L);
        assertEquals(1, tasks.size());
        assertEquals("FAILED", tasks.get(0).getStatus());

        // Ranking recovers -> force nextRetryAt due & run worker
        RankingSyncTask t = tasks.get(0);
        t.setNextRetryAt(LocalDateTime.now().minusSeconds(1));
        rankingSyncTaskRepository.save(t);
        rankingPostStatus = 200;
        int processed = syncRecoveryWorker.processPendingAndRetryableJobs();
        assertEquals(1, processed);
        assertEquals("SYNCED", rankingSyncTaskRepository.findById(t.getId()).orElseThrow().getStatus());

        // Re-confirm, then ADMIN deletes result -> immediately triggers ranking recalculate again!
        mockMvc.perform(put("/api/v1/results/" + resId + "/confirm")
                        .header("Authorization", orgToken))
                .andExpect(status().isOk());

        rankingPostCalls.set(0);
        mockMvc.perform(delete("/api/v1/results/" + resId)
                        .header("Authorization", adminToken))
                .andExpect(status().isNoContent());

        assertEquals(1, rankingPostCalls.get(), "Deleting CONFIRMED result must recalculate ranking");
        assertTrue(resultRepository.findById(resId).isEmpty());
    }

    @Test
    void knockoutDraw_requiresPenaltyShootoutTieBreaker_andConfirmSkipsPointTableRanking() throws Exception {
        String orgToken = issueToken("organizer1", "ROLE_ORGANIZER");

        // Tie 1-1 without penalties -> 400
        mockMvc.perform(post("/api/v1/results")
                        .header("Authorization", orgToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":5,\"scoreParticipant1\":1,\"scoreParticipant2\":1}"))
                .andExpect(status().isBadRequest());

        // Tie 1-1 with equal penalties 4-4 -> 400
        mockMvc.perform(post("/api/v1/results")
                        .header("Authorization", orgToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":5,\"scoreParticipant1\":1,\"scoreParticipant2\":1,\"penaltyScore1\":4,\"penaltyScore2\":4}"))
                .andExpect(status().isBadRequest());

        // Tie 1-1 with deciding penalties 5-3 -> 201, winner decided by penalty
        String resJson = mockMvc.perform(post("/api/v1/results")
                        .header("Authorization", orgToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":5,\"scoreParticipant1\":1,\"scoreParticipant2\":1,\"penaltyScore1\":5,\"penaltyScore2\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.winner").value("Hà Nội"))
                .andExpect(jsonPath("$.result.penaltyScore1").value(5))
                .andExpect(jsonPath("$.result.penaltyScore2").value(3))
                .andReturn().getResponse().getContentAsString();
        long resId = JSON.readTree(resJson).get("result").get("id").asLong();

        // Confirm KNOCKOUT result -> SYNCED, matchSynced=true, rankingSynced=false, Ranking Service NOT called
        rankingPostCalls.set(0);
        mockMvc.perform(put("/api/v1/results/" + resId + "/confirm")
                        .header("Authorization", orgToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.result.syncStatus").value("SYNCED"))
                .andExpect(jsonPath("$.result.matchSynced").value(true))
                .andExpect(jsonPath("$.result.rankingSynced").value(false));
        assertEquals(0, rankingPostCalls.get(), "KNOCKOUT competition must NOT call point-table ranking recalculate");

        // ROUND_ROBIN competition 10 match 11 accepts draw 1-1 without penalties and syncs point-table ranking!
        String rrDrawJson = mockMvc.perform(post("/api/v1/results")
                        .header("Authorization", orgToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":11,\"scoreParticipant1\":1,\"scoreParticipant2\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.winner").value("DRAW"))
                .andReturn().getResponse().getContentAsString();
        long rrResId = JSON.readTree(rrDrawJson).get("result").get("id").asLong();

        mockMvc.perform(put("/api/v1/results/" + rrResId + "/confirm")
                        .header("Authorization", orgToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.result.matchSynced").value(true))
                .andExpect(jsonPath("$.result.rankingSynced").value(true));
        assertEquals(1, rankingPostCalls.get(), "ROUND_ROBIN draw confirm must recalculate ranking points");
    }

    @Test
    void knockoutConfirmedResult_cannotBeReopenedOverwrittenOrDeletedOnceNextRoundExists() throws Exception {
        String orgToken = issueToken("organizer1", "ROLE_ORGANIZER");
        String adminToken = issueToken("admin", "ROLE_ADMIN");

        String resJson = mockMvc.perform(post("/api/v1/results")
                        .header("Authorization", orgToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":5,\"scoreParticipant1\":2,\"scoreParticipant2\":0}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long resId = JSON.readTree(resJson).get("result").get("id").asLong();

        mockMvc.perform(put("/api/v1/results/" + resId + "/confirm")
                        .header("Authorization", orgToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("CONFIRMED"));

        // Vong sau da duoc sinh: doi thang (participant 501) da co mat trong tran #9
        knockoutCompetitionMatches = List.of(
                Map.of("id", 5, "competitionId", 20, "participant1Id", 501, "participant2Id", 502, "status", "FINISHED"),
                Map.of("id", 9, "competitionId", 20, "participant1Id", 501, "participant2Id", 777, "status", "SCHEDULED")
        );

        // Ghi de ket qua -> 409
        mockMvc.perform(post("/api/v1/results")
                        .header("Authorization", orgToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":5,\"scoreParticipant1\":0,\"scoreParticipant2\":3}"))
                .andExpect(status().isConflict());

        // Mo lai (PENDING_CONFIRMATION) -> 409
        mockMvc.perform(patch("/api/v1/results/" + resId)
                        .header("Authorization", orgToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING_CONFIRMATION\"}"))
                .andExpect(status().isConflict());

        // Xoa -> 409
        mockMvc.perform(delete("/api/v1/results/" + resId)
                        .header("Authorization", adminToken))
                .andExpect(status().isConflict());

        // Chi sua ghi chu thi van duoc phep
        mockMvc.perform(patch("/api/v1/results/" + resId)
                        .header("Authorization", orgToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Cap nhat ghi chu\"}"))
                .andExpect(status().isOk());

        Result locked = resultRepository.findById(resId).orElseThrow();
        assertEquals("CONFIRMED", locked.getStatus());
        assertEquals(2, locked.getScoreParticipant1().intValue());
        assertEquals(0, locked.getScoreParticipant2().intValue());

        // Khi tran vong sau da bi huy thi duoc mo lai ket qua
        knockoutCompetitionMatches = List.of(
                Map.of("id", 5, "competitionId", 20, "participant1Id", 501, "participant2Id", 502, "status", "FINISHED"),
                Map.of("id", 9, "competitionId", 20, "participant1Id", 501, "participant2Id", 777, "status", "CANCELLED")
        );
        mockMvc.perform(patch("/api/v1/results/" + resId)
                        .header("Authorization", orgToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING_CONFIRMATION\"}"))
                .andExpect(status().isOk());
        assertEquals("PENDING_CONFIRMATION", resultRepository.findById(resId).orElseThrow().getStatus());
    }
}
