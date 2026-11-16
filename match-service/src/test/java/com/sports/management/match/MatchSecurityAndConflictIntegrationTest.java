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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:match_sec_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class MatchSecurityAndConflictIntegrationTest {

    private static final String JWT_SECRET = "SportsManagementSystemSuperSecretKeyWithAtLeast256BitsLongForHMACSHA256!";

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

    private Match org1Match;
    private Match org2Match;
    private Participant c10p1;
    private Participant c10p2;
    private Participant c10p3;
    private Participant c10p4;
    private Participant c20p1;
    private Participant c20p2;

    @BeforeEach
    void setUp() {
        matchRepository.deleteAll();
        participantRepository.deleteAll();

        when(tournamentClient.tournamentExists(1L)).thenReturn(true);
        when(tournamentClient.tournamentExists(2L)).thenReturn(true);
        when(tournamentClient.getLastHandledInstance()).thenReturn("tournament-service:8081");
        when(tournamentClient.getTournamentById(1L))
                .thenReturn(Map.of(
                        "id", 1, "name", "Giải 1", "organizerUsername", "organizer1",
                        "startDate", "2026-12-01", "endDate", "2026-12-31"));
        when(tournamentClient.getTournamentById(2L))
                .thenReturn(Map.of(
                        "id", 2, "name", "Giải 2", "organizerUsername", "organizer2",
                        "startDate", "2026-12-01", "endDate", "2026-12-31"));
        when(tournamentClient.getCompetitionById(10L))
                .thenReturn(Map.of("id", 10, "tournamentId", 1, "name", "Nội dung 10"));
        when(tournamentClient.getCompetitionById(11L))
                .thenReturn(Map.of("id", 11, "tournamentId", 1, "name", "Nội dung 11"));
        when(tournamentClient.getCompetitionFormat(11L))
                .thenReturn(Map.of("competitionId", 11L, "formatType", "KNOCKOUT", "numberOfGroups", 1, "hasThirdPlaceMatch", false));
        when(tournamentClient.getCompetitionById(20L))
                .thenReturn(Map.of("id", 20, "tournamentId", 2, "name", "Nội dung 20"));

        c10p1 = participantRepository.save(new Participant(null, 10L, null, "TEAM", 1L, "Hà Nội", 1));
        c10p2 = participantRepository.save(new Participant(null, 10L, null, "TEAM", 2L, "Nam Định", 2));
        c10p3 = participantRepository.save(new Participant(null, 10L, null, "TEAM", 3L, "HAGL", 3));
        c10p4 = participantRepository.save(new Participant(null, 10L, null, "TEAM", 4L, "SLNA", 4));
        participantRepository.save(new Participant(null, 11L, null, "TEAM", 11L, "Đội A", 1));
        participantRepository.save(new Participant(null, 11L, null, "TEAM", 12L, "Đội B", 2));
        participantRepository.save(new Participant(null, 11L, null, "TEAM", 13L, "Đội C", 3));
        participantRepository.save(new Participant(null, 11L, null, "TEAM", 14L, "Đội D", 4));
        c20p1 = participantRepository.save(new Participant(null, 20L, null, "TEAM", 5L, "Viettel", 1));
        c20p2 = participantRepository.save(new Participant(null, 20L, null, "TEAM", 6L, "CAHN", 2));

        Match m1 = new Match(null, 1L, "Vòng 1",
                LocalDateTime.of(2026, 12, 10, 18, 0),
                "SVĐ Mỹ Đình", "Hà Nội", "Nam Định", "SCHEDULED");
        m1.setCompetitionId(10L);
        m1.setParticipant1Id(c10p1.getId());
        m1.setParticipant2Id(c10p2.getId());
        m1.setReferee("Trọng Tài Chính");
        m1.setRefereeUsername("referee1");
        m1.setOwnerUsername("organizer1");
        org1Match = matchRepository.save(m1);

        Match m2 = new Match(null, 2L, "Vòng 1",
                LocalDateTime.of(2026, 12, 11, 18, 0),
                "SVĐ Hàng Đẫy", "Viettel", "CAHN", "SCHEDULED");
        m2.setCompetitionId(20L);
        m2.setParticipant1Id(c20p1.getId());
        m2.setParticipant2Id(c20p2.getId());
        m2.setReferee("referee2"); // Display name intentionally matches referee2, NOT username
        m2.setRefereeUsername("referee1"); // Assigned strictly to referee1
        m2.setOwnerUsername("organizer2");
        org2Match = matchRepository.save(m2);
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
    void unauthenticatedAndViewerAreRejectedByRealSecurityFilter() throws Exception {
        mockMvc.perform(post("/api/v1/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":10,\"participant1Id\":" + c10p1.getId() + ",\"participant2Id\":" + c10p2.getId() + ",\"matchTime\":\"2026-12-15T18:00:00\",\"location\":\"San 1\",\"referee\":\"Ref\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/v1/matches/" + org1Match.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ONGOING\"}"))
                .andExpect(status().isUnauthorized());

        String viewerToken = issueToken("user1", "ROLE_VIEWER");
        mockMvc.perform(patch("/api/v1/matches/" + org1Match.getId())
                        .header("Authorization", viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ONGOING\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/matches/" + org1Match.getId())
                        .header("Authorization", viewerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void organizerPermissions_allowOwnedTournamentRejectForeignMoveAndFailClosedWhenUpstreamDown() throws Exception {
        String org1Token = issueToken("organizer1", "ROLE_ORGANIZER");

        // Spoofed ownerUsername in body must NOT elevate or grant access to tournament 2
        mockMvc.perform(post("/api/v1/matches")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":2,\"competitionId\":20,\"ownerUsername\":\"organizer2\",\"round\":\"Vòng 2\",\"participant1Id\":" + c20p1.getId() + ",\"participant2Id\":" + c20p2.getId() + ",\"matchTime\":\"2026-12-20T18:00:00\",\"location\":\"Pleiku\",\"referee\":\"Lân\"}"))
                .andExpect(status().isForbidden());

        // Allowed in own tournament 1, stored ownerUsername is taken from JWT (not spoofed payload)
        mockMvc.perform(post("/api/v1/matches")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":10,\"ownerUsername\":\"spoofedUser\",\"round\":\"Vòng 2\",\"participant1Id\":" + c10p3.getId() + ",\"participant2Id\":" + c10p4.getId() + ",\"matchTime\":\"2026-12-20T18:00:00\",\"location\":\"Pleiku\",\"referee\":\"Lân\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerUsername").value("organizer1"))
                .andExpect(jsonPath("$.participant1").value("HAGL"))
                .andExpect(jsonPath("$.participant2").value("SLNA"));

        // Moving own match (tournament 1) -> foreign tournament 2 must be forbidden
        mockMvc.perform(patch("/api/v1/matches/" + org1Match.getId())
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":2}"))
                .andExpect(status().isForbidden());

        // Moving foreign match (tournament 2) -> own tournament 1 must also be forbidden (source check)
        mockMvc.perform(put("/api/v1/matches/" + org2Match.getId())
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":10,\"round\":\"Vòng 1\",\"participant1Id\":" + c10p1.getId() + ",\"participant2Id\":" + c10p2.getId() + ",\"matchTime\":\"2026-12-11T18:00:00\",\"location\":\"SVĐ Hàng Đẫy\",\"referee\":\"Ref\",\"status\":\"SCHEDULED\"}"))
                .andExpect(status().isForbidden());

        // Deleting foreign tournament's match is forbidden
        mockMvc.perform(delete("/api/v1/matches/" + org2Match.getId())
                        .header("Authorization", org1Token))
                .andExpect(status().isForbidden());

        // Fail closed when tournament-service errors
        when(tournamentClient.getTournamentById(1L))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "tournament-service down"));
        mockMvc.perform(patch("/api/v1/matches/" + org1Match.getId())
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\":\"SVĐ Khác\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void refereePermissions_strictlyRequireAssignedUsernameAndLimitToStatus() throws Exception {
        String assignedRefToken = issueToken("referee1", "ROLE_REFEREE");
        String unassignedRefToken = issueToken("referee2", "ROLE_REFEREE");

        // Unassigned referee is denied even if display name m.referee == "referee2"
        mockMvc.perform(patch("/api/v1/matches/" + org2Match.getId())
                        .header("Authorization", unassignedRefToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ONGOING\"}"))
                .andExpect(status().isForbidden());

        // Assigned referee cannot edit schedule/venue/participants or CANCELLED status
        mockMvc.perform(patch("/api/v1/matches/" + org1Match.getId())
                        .header("Authorization", assignedRefToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\":\"Hack Sân\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/matches/" + org1Match.getId())
                        .header("Authorization", assignedRefToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/matches/" + org1Match.getId())
                        .header("Authorization", assignedRefToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":10,\"round\":\"Vòng 1\",\"matchTime\":\"2026-12-10T18:00:00\",\"location\":\"Đổi sân\",\"referee\":\"Trọng Tài Chính\",\"refereeUsername\":\"referee1\",\"participant1Id\":" + c10p1.getId() + ",\"participant2Id\":" + c10p2.getId() + ",\"status\":\"ONGOING\"}"))
                .andExpect(status().isForbidden());

        // Assigned referee CAN patch status SCHEDULED/POSTPONED -> ONGOING
        mockMvc.perform(patch("/api/v1/matches/" + org1Match.getId())
                        .header("Authorization", assignedRefToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ONGOING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONGOING"));

        // Referee CANNOT set status to FINISHED directly -> 403 Forbidden
        mockMvc.perform(patch("/api/v1/matches/" + org1Match.getId())
                        .header("Authorization", assignedRefToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FINISHED\"}"))
                .andExpect(status().isForbidden());

        // Organizer & Admin CANNOT set FINISHED directly via PATCH or PUT -> 403 Forbidden
        String org1Token = issueToken("organizer1", "ROLE_ORGANIZER");
        String adminToken = issueToken("admin", "ROLE_ADMIN");
        mockMvc.perform(patch("/api/v1/matches/" + org1Match.getId())
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FINISHED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/matches/" + org1Match.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FINISHED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/matches/" + org1Match.getId())
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":10,\"round\":\"Vòng 1\",\"matchTime\":\"2026-12-10T18:00:00\",\"location\":\"SVĐ Mỹ Đình\",\"referee\":\"Trọng Tài Chính\",\"refereeUsername\":\"referee1\",\"participant1Id\":" + c10p1.getId() + ",\"participant2Id\":" + c10p2.getId() + ",\"status\":\"FINISHED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPermissionsAndMandatoryBackendConflictEnforcementWithoutPreflight() throws Exception {
        String adminToken = issueToken("admin", "ROLE_ADMIN");

        // Admin can edit foreign tournament 2 match cleanly
        mockMvc.perform(patch("/api/v1/matches/" + org2Match.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"round\":\"Tứ kết\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.round").value("Tứ kết"));

        // Direct POST without calling /check-conflict -> 409 Conflict (venue + referee overlap with org1Match)
        mockMvc.perform(post("/api/v1/matches")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":10,\"round\":\"Vòng 1\",\"participant1Id\":" + c10p3.getId() + ",\"participant2Id\":" + c10p4.getId() + ",\"matchTime\":\"2026-12-10T18:30:00\",\"location\":\"SVĐ Mỹ Đình\",\"referee\":\"Trọng Tài Chính\",\"refereeUsername\":\"referee1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.conflicts", hasSize(greaterThanOrEqualTo(2))));

        // Direct PUT on self updating only round (same time/venue/referee) must succeed (self excluded!)
        mockMvc.perform(put("/api/v1/matches/" + org1Match.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":10,\"round\":\"Bán kết 1\",\"matchTime\":\"2026-12-10T18:00:00\",\"location\":\"SVĐ Mỹ Đình\",\"referee\":\"Trọng Tài Chính\",\"refereeUsername\":\"referee1\",\"participant1Id\":" + c10p1.getId() + ",\"participant2Id\":" + c10p2.getId() + ",\"status\":\"SCHEDULED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.round").value("Bán kết 1"));

        // Direct PUT moving org2Match into org1Match's slot -> 409
        mockMvc.perform(put("/api/v1/matches/" + org2Match.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":2,\"competitionId\":20,\"round\":\"Tứ kết\",\"matchTime\":\"2026-12-10T18:00:00\",\"location\":\"SVĐ Mỹ Đình\",\"referee\":\"Trọng Tài B\",\"participant1Id\":" + c20p1.getId() + ",\"participant2Id\":" + c20p2.getId() + ",\"status\":\"SCHEDULED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.conflicts", hasSize(greaterThanOrEqualTo(1))));

        // Direct PATCH moving org2Match location/time into org1Match -> 409
        mockMvc.perform(patch("/api/v1/matches/" + org2Match.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchTime\":\"2026-12-10T18:15:00\",\"location\":\"SVĐ Mỹ Đình\"}"))
                .andExpect(status().isConflict());

        // Atomic generate-schedule conflict check: conflicts with blocking match at "SVĐ Quốc Gia Mỹ Đình" 18:00
        Match blocking = new Match(null, 1L, "Blocking",
                LocalDateTime.of(2026, 12, 25, 18, 0),
                "SVĐ Quốc Gia Mỹ Đình", "Đội X", "Đội Y", "SCHEDULED");
        blocking.setCompetitionId(10L);
        blocking.setParticipant1Id(9001L);
        blocking.setParticipant2Id(9002L);
        matchRepository.save(blocking);
        long countBefore = matchRepository.count();

        mockMvc.perform(post("/api/v1/matches/generate-schedule")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":11,\"startDate\":\"2026-12-25\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.conflicts", hasSize(greaterThanOrEqualTo(1))));

        assertEquals(countBefore, matchRepository.count(),
                "generate-schedule must not persist partial matches when a conflict occurs");

        // Admin delete owned/scheduled match succeeds
        mockMvc.perform(delete("/api/v1/matches/" + org2Match.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isNoContent());
        assertNotEquals(countBefore, matchRepository.count());
    }

    @Test
    void internalResultSyncScope_canOnlyPatchStatusToFinishedAndIsDeniedAllOtherOperations() throws Exception {
        String internalSyncToken = issueToken("result-sync-worker", "ROLE_INTERNAL_RESULT_SYNC");

        // Denied POST create
        mockMvc.perform(post("/api/v1/matches")
                        .header("Authorization", internalSyncToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"participant1\":\"A\",\"participant2\":\"B\",\"matchTime\":\"2026-12-15T18:00:00\",\"location\":\"San 1\",\"referee\":\"Ref\"}"))
                .andExpect(status().isForbidden());

        // Denied DELETE
        mockMvc.perform(delete("/api/v1/matches/" + org1Match.getId())
                        .header("Authorization", internalSyncToken))
                .andExpect(status().isForbidden());

        // Denied PATCH non-status fields or non-FINISHED status
        mockMvc.perform(patch("/api/v1/matches/" + org1Match.getId())
                        .header("Authorization", internalSyncToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\":\"Hack\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/matches/" + org1Match.getId())
                        .header("Authorization", internalSyncToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isForbidden());

        // Allowed PATCH status=FINISHED with scores ONLY via ROLE_INTERNAL_RESULT_SYNC
        mockMvc.perform(patch("/api/v1/matches/" + org1Match.getId())
                        .header("Authorization", internalSyncToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FINISHED\",\"scoreParticipant1\":3,\"scoreParticipant2\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));
    }
}
