package com.sports.management.match;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sports.management.match.client.TournamentClient;
import com.sports.management.match.model.Match;
import com.sports.management.match.model.Participant;
import com.sports.management.match.repository.MatchRepository;
import com.sports.management.match.repository.ParticipantRepository;
import com.sports.management.messaging.producer.MatchEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:match_a4_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "jwt.secret=SportsManagementSystemSuperSecretKeyWithAtLeast256BitsLongForHMACSHA256!"
})
@AutoConfigureMockMvc
class MatchScheduleAndParticipantIntegrationTest {

    private static final String JWT_SECRET = "SportsManagementSystemSuperSecretKeyWithAtLeast256BitsLongForHMACSHA256!";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @MockitoBean
    private TournamentClient tournamentClient;

    @MockitoBean
    private MatchEventProducer matchEventProducer;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        matchRepository.deleteAll();
        participantRepository.deleteAll();
        adminToken = issueToken("admin", "ROLE_ADMIN");

        when(tournamentClient.tournamentExists(1L)).thenReturn(true);
        when(tournamentClient.tournamentExists(2L)).thenReturn(true);
        when(tournamentClient.getLastHandledInstance()).thenReturn("tournament-service:8081");
        when(tournamentClient.getTournamentById(1L)).thenReturn(Map.of(
                "id", 1, "name", "Giải VĐQG", "organizerUsername", "organizer1",
                "startDate", "2026-10-10", "endDate", "2026-12-20"
        ));
        when(tournamentClient.getTournamentById(2L)).thenReturn(Map.of(
                "id", 2, "name", "Giải Khác", "organizerUsername", "organizer2",
                "startDate", "2026-10-10", "endDate", "2026-12-20"
        ));

        for (long cid = 100L; cid <= 110L; cid++) {
            when(tournamentClient.getCompetitionById(cid))
                    .thenReturn(Map.of("id", cid, "tournamentId", 1, "name", "Nội dung " + cid));
            String fmt = (cid == 101L) ? "ROUND_ROBIN" : "KNOCKOUT";
            when(tournamentClient.getCompetitionFormat(cid))
                    .thenReturn(Map.of("competitionId", cid, "formatType", fmt, "numberOfGroups", 1, "hasThirdPlaceMatch", false));
        }
        when(tournamentClient.getCompetitionFormat(108L))
                .thenReturn(Map.of("competitionId", 108L, "formatType", "GROUP_STAGE", "numberOfGroups", 3, "hasThirdPlaceMatch", false));
        when(tournamentClient.getCompetitionFormat(109L))
                .thenReturn(Map.of("competitionId", 109L, "formatType", "HYBRID", "numberOfGroups", 2, "hasThirdPlaceMatch", false));
        when(tournamentClient.getCompetitionById(200L))
                .thenReturn(Map.of("id", 200, "tournamentId", 2, "name", "Nội dung Giải 2"));
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

    private List<Participant> seedParticipants(long compId, int count, String prefix) {
        List<Participant> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(participantRepository.save(
                    new Participant(null, compId, null, "TEAM", (long) (compId * 100 + i), prefix + " " + i, i)));
        }
        return list;
    }

    @Test
    void createAndModifyMatch_enforcesParticipantBindingTournamentDatesAndTransitions() throws Exception {
        List<Participant> comp100 = seedParticipants(100L, 2, "CLB");
        Participant foreignComp = participantRepository.save(
                new Participant(null, 200L, null, "TEAM", 999L, "Đội Giải 2", 1));

        // 1. Missing required @Valid fields -> 400 with field errors
        mockMvc.perform(post("/api/v1/matches")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.competitionId").exists())
                .andExpect(jsonPath("$.errors.matchTime").exists())
                .andExpect(jsonPath("$.errors.participant1Id").exists())
                .andExpect(jsonPath("$.errors.participant2Id").exists());

        // 2. Competition belonging to tournament 2 used under tournament 1 -> 400
        mockMvc.perform(post("/api/v1/matches")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":200,\"participant1Id\":" + foreignComp.getId()
                                + ",\"participant2Id\":" + comp100.get(1).getId() + ",\"matchTime\":\"2026-11-01T18:00:00\"}"))
                .andExpect(status().isBadRequest());

        // 3. Participant belonging to another competition -> 400
        mockMvc.perform(post("/api/v1/matches")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":100,\"participant1Id\":" + comp100.get(0).getId()
                                + ",\"participant2Id\":" + foreignComp.getId() + ",\"matchTime\":\"2026-11-01T18:00:00\"}"))
                .andExpect(status().isBadRequest());

        // 4. matchTime outside [2026-10-10, 2026-12-20] -> 400
        mockMvc.perform(post("/api/v1/matches")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":100,\"participant1Id\":" + comp100.get(0).getId()
                                + ",\"participant2Id\":" + comp100.get(1).getId() + ",\"matchTime\":\"2026-10-09T18:00:00\"}"))
                .andExpect(status().isBadRequest());

        // 5. Valid create auto-fills displayName from Participant & defaults status SCHEDULED
        MvcResult createRes = mockMvc.perform(post("/api/v1/matches")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":100,\"round\":\"Vòng 1\","
                                + "\"participant1Id\":" + comp100.get(0).getId()
                                + ",\"participant2Id\":" + comp100.get(1).getId()
                                + ",\"participant1\":\"Fake Name\",\"participant2\":\"Other Fake\","
                                + "\"location\":\"SVĐ Mỹ Đình\",\"matchTime\":\"2026-11-15T19:00:00\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.participant1").value("CLB 1"))
                .andExpect(jsonPath("$.participant2").value("CLB 2"))
                .andReturn();
        long matchId = MAPPER.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // 6. Invalid status & illegal transitions
        mockMvc.perform(patch("/api/v1/matches/" + matchId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INVALID_STATE\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/v1/matches/" + matchId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ONGOING\"}"))
                .andExpect(status().isOk());

        // ONGOING -> SCHEDULED rejected
        mockMvc.perform(patch("/api/v1/matches/" + matchId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SCHEDULED\"}"))
                .andExpect(status().isBadRequest());

        // ONGOING -> FINISHED rejected for ADMIN/ORGANIZER directly (403); allowed ONLY via ROLE_INTERNAL_RESULT_SYNC!
        mockMvc.perform(patch("/api/v1/matches/" + matchId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FINISHED\"}"))
                .andExpect(status().isForbidden());

        String internalSyncToken = issueToken("result-sync-worker", "ROLE_INTERNAL_RESULT_SYNC");
        mockMvc.perform(patch("/api/v1/matches/" + matchId)
                        .header("Authorization", internalSyncToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FINISHED\",\"scoreParticipant1\":2,\"scoreParticipant2\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));

        // FINISHED -> SCHEDULED rejected
        mockMvc.perform(patch("/api/v1/matches/" + matchId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SCHEDULED\"}"))
                .andExpect(status().isBadRequest());

        // FINISHED match cannot change venue, time, or participants
        mockMvc.perform(patch("/api/v1/matches/" + matchId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\":\"SVĐ Khác\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateSchedule_usesCircleRotationForRoundRobin_reportsKnockoutBye_andRejectsDuplicateWith409() throws Exception {
        // Missing competitionId must return 400 (never default to 1L)
        mockMvc.perform(post("/api/v1/matches/generate-schedule")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1}"))
                .andExpect(status().isBadRequest());

        // Sending client format differing from configured competition format -> 400 explaining single source of truth
        seedParticipants(100L, 4, "DefaultKO");
        mockMvc.perform(post("/api/v1/matches/generate-schedule")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":100,\"format\":\"ROUND_ROBIN\",\"startDate\":\"2026-10-12\"}"))
                .andExpect(status().isBadRequest());

        // Competition left on default KNOCKOUT format without passing format -> schedules KNOCKOUT directly
        mockMvc.perform(post("/api/v1/matches/generate-schedule")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":100,\"startDate\":\"2026-10-12\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.format").value("KNOCKOUT"))
                .andExpect(jsonPath("$.generatedCount").value(2))
                .andExpect(jsonPath("$.matches[0].round", startsWith("Bán kết")));

        // 4 participants configured ROUND_ROBIN -> Circle Round Robin: 6 matches across 3 distinct days, max 1 match per team per day/round
        seedParticipants(101L, 4, "RR Team");
        MvcResult rrRes = mockMvc.perform(post("/api/v1/matches/generate-schedule")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":101,"
                                + "\"startDate\":\"2026-10-15\",\"teams\":[\"Ignored Client Team\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.format").value("ROUND_ROBIN"))
                .andExpect(jsonPath("$.generatedCount").value(6))
                .andExpect(jsonPath("$.matches", hasSize(6)))
                .andReturn();

        JsonNode rrMatches = MAPPER.readTree(rrRes.getResponse().getContentAsString()).get("matches");
        Map<LocalDate, Set<Long>> teamsPlayedByDate = new HashMap<>();
        for (JsonNode m : rrMatches) {
            long p1Id = m.get("participant1Id").asLong();
            long p2Id = m.get("participant2Id").asLong();
            assertNotEquals(p1Id, p2Id);
            assertTrue(m.get("participant1").asText().startsWith("RR Team"));
            LocalDate day = LocalDate.parse(m.get("matchTime").asText().substring(0, 10));
            Set<Long> seen = teamsPlayedByDate.computeIfAbsent(day, k -> new HashSet<>());
            assertTrue(seen.add(p1Id), "Team " + p1Id + " played more than once on " + day);
            assertTrue(seen.add(p2Id), "Team " + p2Id + " played more than once on " + day);
        }

        // Calling generate-schedule again on same competition -> 409 Conflict
        mockMvc.perform(post("/api/v1/matches/generate-schedule")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":101,\"startDate\":\"2026-10-15\"}"))
                .andExpect(status().isConflict());

        // 5 teams KNOCKOUT -> 2 matches labeled "Tứ kết" and 1 bye participant explicitly reported
        seedParticipants(102L, 5, "KO5");
        mockMvc.perform(post("/api/v1/matches/generate-schedule")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":102,\"startDate\":\"2026-11-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.generatedCount").value(2))
                .andExpect(jsonPath("$.matches[0].round", startsWith("Tứ kết")))
                .andExpect(jsonPath("$.byeParticipants", hasSize(1)))
                .andExpect(jsonPath("$.byeParticipants[0].displayName").value("KO5 5"));

        // GROUP_STAGE with numberOfGroups = 3 and 6 participants -> splits across Bảng A, Bảng B, Bảng C (not fixed 2!)
        seedParticipants(108L, 6, "GS Team");
        MvcResult gsRes = mockMvc.perform(post("/api/v1/matches/generate-schedule")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":108,\"startDate\":\"2026-11-22\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.format").value("GROUP_STAGE"))
                .andExpect(jsonPath("$.generatedCount").value(3))
                .andReturn();
        Set<String> groupNames = new HashSet<>();
        for (JsonNode m : MAPPER.readTree(gsRes.getResponse().getContentAsString()).get("matches")) {
            groupNames.add(m.get("groupName").asText());
        }
        assertEquals(Set.of("Bảng A", "Bảng B", "Bảng C"), groupNames);

        // HYBRID format returns 400 explicitly instead of falling back to ROUND_ROBIN
        seedParticipants(109L, 4, "Hybrid Team");
        mockMvc.perform(post("/api/v1/matches/generate-schedule")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":109,\"startDate\":\"2026-11-28\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("chưa hỗ trợ xếp lịch tự động")));

        // Verify exact knockout round labels for 16 -> Vòng 1/8, 8 -> Tứ kết, 4 -> Bán kết, 2 -> Chung kết
        seedParticipants(103L, 16, "K16");
        mockMvc.perform(post("/api/v1/matches/generate-schedule")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":103,\"startDate\":\"2026-11-05\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matches[0].round", startsWith("Vòng 1/8")));

        seedParticipants(104L, 8, "K8");
        mockMvc.perform(post("/api/v1/matches/generate-schedule")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":104,\"startDate\":\"2026-11-10\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matches[0].round", startsWith("Tứ kết")));

        seedParticipants(105L, 4, "K4");
        mockMvc.perform(post("/api/v1/matches/generate-schedule")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":105,\"startDate\":\"2026-11-15\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matches[0].round", startsWith("Bán kết")));

        seedParticipants(106L, 2, "K2");
        mockMvc.perform(post("/api/v1/matches/generate-schedule")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":106,\"startDate\":\"2026-11-20\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matches[0].round", startsWith("Chung kết")));
    }
}
