package com.sports.management.match;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sports.management.exception.GlobalExceptionHandler;
import com.sports.management.match.client.TournamentClient;
import com.sports.management.match.model.Match;
import com.sports.management.match.model.Participant;
import com.sports.management.match.model.Registration;
import com.sports.management.match.repository.MatchRepository;
import com.sports.management.match.repository.ParticipantRepository;
import com.sports.management.match.repository.RegistrationRepository;
import com.sports.management.messaging.producer.MatchEventProducer;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:match_a5_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class RegistrationSecurityAndBusinessTest {

    private static final String JWT_SECRET = "SportsManagementSystemSuperSecretKeyWithAtLeast256BitsLongForHMACSHA256!";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private MatchRepository matchRepository;

    @MockitoBean
    private TournamentClient tournamentClient;

    @MockitoBean
    private MatchEventProducer matchEventProducer;

    private String mgr1Token;
    private String mgr2Token;
    private String org1Token;
    private String org2Token;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        matchRepository.deleteAll();
        participantRepository.deleteAll();
        registrationRepository.deleteAll();

        mgr1Token = issueToken("manager1", "ROLE_TEAM_MANAGER");
        mgr2Token = issueToken("manager2", "ROLE_TEAM_MANAGER");
        org1Token = issueToken("organizer1", "ROLE_ORGANIZER");
        org2Token = issueToken("organizer2", "ROLE_ORGANIZER");
        adminToken = issueToken("admin", "ROLE_ADMIN");

        LocalDate today = LocalDate.now();
        when(tournamentClient.getTournamentById(1L)).thenReturn(Map.of(
                "id", 1, "name", "Open Tour", "organizerUsername", "organizer1",
                "registrationStartDate", today.minusDays(5).toString(),
                "registrationEndDate", today.plusDays(5).toString(),
                "startDate", today.toString(),
                "endDate", today.plusDays(30).toString()
        ));
        when(tournamentClient.getTournamentById(2L)).thenReturn(Map.of(
                "id", 2, "name", "Closed Tour", "organizerUsername", "organizer1",
                "registrationStartDate", today.minusDays(20).toString(),
                "registrationEndDate", today.minusDays(2).toString(),
                "startDate", today.toString(),
                "endDate", today.plusDays(20).toString()
        ));

        when(tournamentClient.getSportById(1L)).thenReturn(Map.of("id", 1, "name", "Bóng đá", "sportType", "TEAM"));
        when(tournamentClient.getSportById(2L)).thenReturn(Map.of("id", 2, "name", "Cầu lông đơn", "sportType", "INDIVIDUAL"));

        when(tournamentClient.getCompetitionById(10L)).thenReturn(Map.of(
                "id", 10, "tournamentId", 1, "sportId", 1, "name", "Bóng đá Nam", "status", "OPEN_REGISTRATION"));
        when(tournamentClient.getCompetitionById(11L)).thenReturn(Map.of(
                "id", 11, "tournamentId", 1, "sportId", 2, "name", "Đơn Nam", "status", "OPEN_REGISTRATION"));
        when(tournamentClient.getCompetitionById(12L)).thenReturn(Map.of(
                "id", 12, "tournamentId", 1, "sportId", 1, "name", "Đang đóng", "status", "SCHEDULED"));
        when(tournamentClient.getCompetitionById(20L)).thenReturn(Map.of(
                "id", 20, "tournamentId", 2, "sportId", 1, "name", "Hết hạn", "status", "OPEN_REGISTRATION"));

        when(tournamentClient.getCompetitionRule(10L)).thenReturn(Map.of(
                "competitionId", 10, "maxParticipants", 1, "minTeamMembers", 2, "maxTeamMembers", 5));
        when(tournamentClient.getCompetitionRule(11L)).thenReturn(Map.of(
                "competitionId", 11, "maxParticipants", 8, "minTeamMembers", 1, "maxTeamMembers", 1));

        when(tournamentClient.getTeamById(100L)).thenReturn(Map.of(
                "id", 100, "name", "CLB Manager 1", "ownerUsername", "manager1"));
        when(tournamentClient.getTeamById(101L)).thenReturn(Map.of(
                "id", 101, "name", "CLB Manager 2", "ownerUsername", "manager2"));
        when(tournamentClient.getTeamMembers(100L)).thenReturn(List.of(
                Map.of("id", 1, "athleteId", 200), Map.of("id", 2, "athleteId", 202)));
        when(tournamentClient.getTeamMembers(101L)).thenReturn(List.of(
                Map.of("id", 3, "athleteId", 203), Map.of("id", 4, "athleteId", 204)));

        when(tournamentClient.getAthleteById(200L)).thenReturn(Map.of(
                "id", 200, "fullName", "Nguyễn Tiến Minh", "ownerUsername", "manager1", "healthStatus", "QUALIFIED"));
        when(tournamentClient.getAthleteById(201L)).thenReturn(Map.of(
                "id", 201, "fullName", "VĐV Chấn Thương", "ownerUsername", "manager1", "healthStatus", "INJURED"));
        when(tournamentClient.getAthleteById(202L)).thenReturn(Map.of(
                "id", 202, "fullName", "Nguyễn Quang Hải", "ownerUsername", "manager1", "healthStatus", "QUALIFIED"));
        when(tournamentClient.getAthleteById(203L)).thenReturn(Map.of(
                "id", 203, "fullName", "Đỗ Hùng Dũng", "ownerUsername", "manager2", "healthStatus", "QUALIFIED"));
        when(tournamentClient.getAthleteById(204L)).thenReturn(Map.of(
                "id", 204, "fullName", "Nguyễn Hoàng Đức", "ownerUsername", "manager2", "healthStatus", "QUALIFIED"));
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
    void register_enforcesOwnershipDatesStatusSportTypeTeamMembersHealthAndConflict409() throws Exception {
        // 1. Wrong owner team for manager1 -> 403
        mockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competitionId\":10,\"participantType\":\"TEAM\",\"participantReferenceId\":101}"))
                .andExpect(status().isForbidden());

        // 2. Closed registration window -> 400
        mockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competitionId\":20,\"participantType\":\"TEAM\",\"participantReferenceId\":100}"))
                .andExpect(status().isBadRequest());

        // 3. Competition status != OPEN_REGISTRATION -> 400
        mockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competitionId\":12,\"participantType\":\"TEAM\",\"participantReferenceId\":100}"))
                .andExpect(status().isBadRequest());

        // 4. SportType TEAM rejects ATHLETE -> 400; INDIVIDUAL rejects TEAM -> 400
        mockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competitionId\":10,\"participantType\":\"ATHLETE\",\"participantReferenceId\":200}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competitionId\":11,\"participantType\":\"TEAM\",\"participantReferenceId\":100}"))
                .andExpect(status().isBadRequest());

        // 5. Athlete health != QUALIFIED -> 400
        mockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competitionId\":11,\"participantType\":\"ATHLETE\",\"participantReferenceId\":201}"))
                .andExpect(status().isBadRequest());

        // 6. Team below minTeamMembers -> 400
        when(tournamentClient.getTeamMembers(100L)).thenReturn(List.of(Map.of("id", 1, "athleteId", 200)));
        mockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competitionId\":10,\"participantType\":\"TEAM\",\"participantReferenceId\":100}"))
                .andExpect(status().isBadRequest());
        when(tournamentClient.getTeamMembers(100L)).thenReturn(List.of(
                Map.of("id", 1, "athleteId", 200), Map.of("id", 2, "athleteId", 202)));

        // 7. Valid register reads team "name" (not teamName) & sets registeredBy from JWT
        mockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competitionId\":10,\"participantType\":\"TEAM\",\"participantReferenceId\":100,\"registeredBy\":\"spoof\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.participantName").value("CLB Manager 1"))
                .andExpect(jsonPath("$.registeredBy").value("manager1"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // 8. Duplicate registration -> 409 Conflict (not 400!)
        mockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competitionId\":10,\"participantType\":\"TEAM\",\"participantReferenceId\":100}"))
                .andExpect(status().isConflict());

        // 9. ADMIN can register on behalf of another manager's team
        mockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competitionId\":10,\"participantType\":\"TEAM\",\"participantReferenceId\":101}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.participantName").value("CLB Manager 2"))
                .andExpect(jsonPath("$.registeredBy").value("admin"));
    }

    @Test
    void approveRejectWithdrawAndReRegister_enforceOwnershipCapacityParticipantCleanupAndMatchConflict() throws Exception {
        // Submit 2 registrations for comp 10 (maxParticipants = 1)
        MvcResult r1Res = mockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competitionId\":10,\"participantType\":\"TEAM\",\"participantReferenceId\":100}"))
                .andExpect(status().isCreated())
                .andReturn();
        long reg1Id = MAPPER.readTree(r1Res.getResponse().getContentAsString()).get("id").asLong();

        MvcResult r2Res = mockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", mgr2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competitionId\":10,\"participantType\":\"TEAM\",\"participantReferenceId\":101}"))
                .andExpect(status().isCreated())
                .andReturn();
        long reg2Id = MAPPER.readTree(r2Res.getResponse().getContentAsString()).get("id").asLong();

        // Wrong organizer cannot approve/reject -> 403
        mockMvc.perform(put("/api/v1/registrations/" + reg1Id + "/approve")
                        .header("Authorization", org2Token))
                .andExpect(status().isForbidden());

        // Owner organizer approves reg1 -> 200 & creates Participant
        mockMvc.perform(put("/api/v1/registrations/" + reg1Id + "/approve")
                        .header("Authorization", org1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        assertEquals(1, participantRepository.findByCompetitionId(10L).size());

        // Approving non-PENDING reg1 again -> 400; exceeding maxParticipants (1) for reg2 -> 409
        mockMvc.perform(put("/api/v1/registrations/" + reg1Id + "/approve")
                        .header("Authorization", org1Token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/registrations/" + reg2Id + "/approve")
                        .header("Authorization", org1Token))
                .andExpect(status().isConflict());

        // Wrong user cannot withdraw reg1 -> 403
        mockMvc.perform(put("/api/v1/registrations/" + reg1Id + "/withdraw")
                        .header("Authorization", mgr2Token))
                .andExpect(status().isForbidden());

        // Rejecting approved reg1 BEFORE any matches deletes Participant -> 200
        mockMvc.perform(put("/api/v1/registrations/" + reg1Id + "/reject")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Chưa đạt chuẩn\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
        assertTrue(participantRepository.findByCompetitionId(10L).isEmpty());

        // REJECTED registration can re-register -> 201 & PENDING
        mockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competitionId\":10,\"participantType\":\"TEAM\",\"participantReferenceId\":100}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Owner withdraws reg2 -> WITHDRAWN, then can re-register -> PENDING
        mockMvc.perform(put("/api/v1/registrations/" + reg2Id + "/withdraw")
                        .header("Authorization", mgr2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));
        mockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", mgr2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competitionId\":10,\"participantType\":\"TEAM\",\"participantReferenceId\":101}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Re-approve reg1, create a scheduled Match referencing Participant -> reject/withdraw blocked with 409!
        mockMvc.perform(put("/api/v1/registrations/" + reg1Id + "/approve")
                        .header("Authorization", org1Token))
                .andExpect(status().isOk());
        Participant p1 = participantRepository.findByCompetitionId(10L).get(0);
        Match m = new Match(null, 1L, "Chung kết", LocalDateTime.now().plusDays(1), "Sân 1", p1.getDisplayName(), "Khác", "SCHEDULED");
        m.setCompetitionId(10L);
        m.setParticipant1Id(p1.getId());
        m.setParticipant2Id(9999L);
        matchRepository.save(m);

        mockMvc.perform(put("/api/v1/registrations/" + reg1Id + "/reject")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Muộn\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/v1/registrations/" + reg1Id + "/withdraw")
                        .header("Authorization", mgr1Token))
                .andExpect(status().isConflict());
    }

    @Test
    void globalExceptionHandler_mapsDataIntegrityTo409AndConstraintViolationTo400() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        var integrityResp = handler.handleDataIntegrityViolation(new DataIntegrityViolationException("duplicate key"));
        assertEquals(409, integrityResp.getStatusCode().value());

        var constraintResp = handler.handleConstraintViolation(new ConstraintViolationException("invalid field", Set.of()));
        assertEquals(400, constraintResp.getStatusCode().value());
    }

    @Test
    void registerAndApprove_lockAfterMatchesExist_enforceRuleDeadline_andValidateTeamMemberHealthAndOverlap() throws Exception {
        // 1. Respect CompetitionRule.registrationDeadline when passed (even if tournament window is still open)
        when(tournamentClient.getCompetitionRule(10L)).thenReturn(Map.of(
                "competitionId", 10, "maxParticipants", 8, "minTeamMembers", 2, "maxTeamMembers", 5,
                "registrationDeadline", LocalDateTime.now().minusHours(1).toString()
        ));
        mockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competitionId\":10,\"participantType\":\"TEAM\",\"participantReferenceId\":100}"))
                .andExpect(status().isBadRequest());

        // Restore valid deadline for comp 10
        when(tournamentClient.getCompetitionRule(10L)).thenReturn(Map.of(
                "competitionId", 10, "maxParticipants", 8, "minTeamMembers", 2, "maxTeamMembers", 5,
                "registrationDeadline", LocalDateTime.now().plusDays(2).toString()
        ));

        // 2. Team containing non-QUALIFIED member (athlete 201 INJURED) -> 400 Bad Request
        when(tournamentClient.getTeamMembers(100L)).thenReturn(List.of(
                Map.of("id", 1, "athleteId", 200), Map.of("id", 2, "athleteId", 201)));
        mockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competitionId\":10,\"participantType\":\"TEAM\",\"participantReferenceId\":100}"))
                .andExpect(status().isBadRequest());
        when(tournamentClient.getTeamMembers(100L)).thenReturn(List.of(
                Map.of("id", 1, "athleteId", 200), Map.of("id", 2, "athleteId", 202)));

        // 3. Register team 100 successfully (athlete 200, 202), then team 101 shares athlete 202 -> 409 Conflict
        mockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competitionId\":10,\"participantType\":\"TEAM\",\"participantReferenceId\":100}"))
                .andExpect(status().isCreated());

        when(tournamentClient.getTeamMembers(101L)).thenReturn(List.of(
                Map.of("id", 3, "athleteId", 202), Map.of("id", 4, "athleteId", 204)));
        mockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", mgr2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competitionId\":10,\"participantType\":\"TEAM\",\"participantReferenceId\":101}"))
                .andExpect(status().isConflict());

        // Fix team 101 distinct athlete 203 -> team 101 registers cleanly
        when(tournamentClient.getTeamMembers(101L)).thenReturn(List.of(
                Map.of("id", 3, "athleteId", 203), Map.of("id", 4, "athleteId", 204)));
        MvcResult reg2Pending = mockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", mgr2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competitionId\":10,\"participantType\":\"TEAM\",\"participantReferenceId\":101}"))
                .andExpect(status().isCreated())
                .andReturn();
        long reg2PendingId = MAPPER.readTree(reg2Pending.getResponse().getContentAsString()).get("id").asLong();

        // 4. Once a match exists in competition 10 -> BOTH new register AND approveRegistration return 409 Conflict!
        Match match = new Match(null, 1L, "Chung kết", LocalDateTime.now().plusDays(1), "Sân A", "Đội A", "Đội B", "SCHEDULED");
        match.setCompetitionId(10L);
        match.setParticipant1Id(1L);
        match.setParticipant2Id(2L);
        matchRepository.save(match);

        mockMvc.perform(put("/api/v1/registrations/" + reg2PendingId + "/approve")
                        .header("Authorization", org1Token))
                .andExpect(status().isConflict());

        when(tournamentClient.getTeamById(102L)).thenReturn(Map.of(
                "id", 102, "name", "CLB Mới", "ownerUsername", "manager1"));
        when(tournamentClient.getTeamMembers(102L)).thenReturn(List.of(
                Map.of("id", 5, "athleteId", 200), Map.of("id", 6, "athleteId", 204)));
        mockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competitionId\":10,\"participantType\":\"TEAM\",\"participantReferenceId\":102}"))
                .andExpect(status().isConflict());
    }
}
