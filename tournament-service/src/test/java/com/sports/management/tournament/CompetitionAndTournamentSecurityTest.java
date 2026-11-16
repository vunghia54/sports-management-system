package com.sports.management.tournament;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sports.management.tournament.model.Competition;
import com.sports.management.tournament.model.Sport;
import com.sports.management.tournament.model.Tournament;
import com.sports.management.tournament.repository.CompetitionFormatRepository;
import com.sports.management.tournament.repository.CompetitionRepository;
import com.sports.management.tournament.repository.CompetitionRuleRepository;
import com.sports.management.tournament.repository.SportRepository;
import com.sports.management.tournament.repository.TournamentRepository;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CompetitionAndTournamentSecurityTest {

    private static final String JWT_SECRET = "SportsManagementSystemSuperSecretKeyWithAtLeast256BitsLongForHMACSHA256!";

    private static HttpServer mockServer;
    private static int mockPort;
    private static final AtomicReference<String> matchMode = new AtomicReference<>("EMPTY"); // EMPTY, HAS_MATCH, HAS_REG, DOWN
    private static final AtomicInteger matchStatusOverride = new AtomicInteger(200);
    private static final AtomicReference<String> rankingsBody = new AtomicReference<>("[]");

    static {
        try {
            mockServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            mockPort = mockServer.getAddress().getPort();
            mockServer.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                int status = 200;
                String body = "[]";
                if (path.startsWith("/api/v1/matches")) {
                    String mode = matchMode.get();
                    if ("DOWN".equals(mode)) {
                        status = 500;
                        body = "{\"message\":\"match down\"}";
                    } else if ("HAS_MATCH".equals(mode)) {
                        body = "[{\"id\":901,\"status\":\"SCHEDULED\"}]";
                    } else {
                        status = matchStatusOverride.get();
                    }
                } else if (path.startsWith("/api/v1/registrations")) {
                    String mode = matchMode.get();
                    if ("DOWN".equals(mode)) {
                        status = 500;
                        body = "{\"message\":\"reg down\"}";
                    } else if ("HAS_REG".equals(mode)) {
                        body = "[{\"id\":701,\"status\":\"PENDING\"}]";
                    }
                } else if (path.equals("/api/v1/rankings")) {
                    body = rankingsBody.get();
                }
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            });
            mockServer.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    static void stopMockServer() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        String baseUrl = "http://127.0.0.1:" + mockPort;
        registry.add("services.match.url", () -> baseUrl);
        registry.add("services.ranking.url", () -> baseUrl);
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:b1testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("jwt.secret", () -> JWT_SECRET);
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TournamentRepository tournamentRepository;
    @Autowired
    private CompetitionRepository competitionRepository;
    @Autowired
    private CompetitionRuleRepository ruleRepository;
    @Autowired
    private CompetitionFormatRepository formatRepository;
    @Autowired
    private SportRepository sportRepository;

    private Sport sport;

    @BeforeEach
    void setUp() {
        matchMode.set("EMPTY");
        matchStatusOverride.set(200);
        rankingsBody.set("[]");
        formatRepository.deleteAll();
        ruleRepository.deleteAll();
        competitionRepository.deleteAll();
        tournamentRepository.deleteAll();
        sport = sportRepository.findAll().stream().findFirst().orElseGet(() -> {
            Sport s = new Sport();
            s.setName("Bóng đá 7 người Test");
            s.setSportType("TEAM");
            return sportRepository.save(s);
        });
    }

    private String token(String username, String role) throws Exception {
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

    private Tournament createTournamentEntity(String name, String owner) {
        Tournament t = new Tournament();
        t.setName(name);
        t.setLocation("Hà Nội");
        t.setOrganizerUsername(owner);
        t.setStatus("OPEN_REGISTRATION");
        t.setRegistrationStartDate(LocalDate.of(2026, 11, 1));
        t.setRegistrationEndDate(LocalDate.of(2026, 11, 16));
        t.setStartDate(LocalDate.of(2026, 11, 16));
        t.setEndDate(LocalDate.of(2026, 12, 20));
        return tournamentRepository.save(t);
    }

    @Test
    void createTournament_RejectsSpoofingOtherOrganizer_AndNullOwnerLockedToAdmin() throws Exception {
        String org1Token = token("organizer1", "ROLE_ORGANIZER");
        String org2Token = token("organizer2", "ROLE_ORGANIZER");
        String adminToken = token("admin", "ROLE_ADMIN");

        // Organizer cannot spoof organizerUsername as someone else
        String spoofPayload = """
                {
                  "name": "Giai Gia Mao",
                  "location": "Hà Nội",
                  "organizerUsername": "organizer2",
                  "registrationStartDate": "2026-11-01",
                  "registrationEndDate": "2026-11-16",
                  "status": "OPEN_REGISTRATION",
                  "startDate": "2026-11-20",
                  "endDate": "2026-11-30"
                }
                """;
        mockMvc.perform(post("/api/v1/tournaments")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spoofPayload))
                .andExpect(status().isForbidden());

        // Legacy tournament without organizerUsername -> forbidden to non-admin, editable by admin
        Tournament legacy = createTournamentEntity("Legacy Tour", null);
        String updatePayload = """
                {
                  "name": "Legacy Renamed",
                  "location": "Hà Nội",
                  "registrationStartDate": "2026-11-01",
                  "registrationEndDate": "2026-11-16",
                  "status": "ONGOING",
                  "startDate": "2026-11-20",
                  "endDate": "2026-11-30"
                }
                """;
        mockMvc.perform(put("/api/v1/tournaments/" + legacy.getId())
                        .header("Authorization", org2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/tournaments/" + legacy.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Legacy Renamed")));
    }

    @Test
    void tournamentAndCompetitionStatusValidation_RejectsInvalidValuesWith400() throws Exception {
        String org1Token = token("organizer1", "ROLE_ORGANIZER");
        Tournament owned = createTournamentEntity("Valid Tour", "organizer1");

        String invalidTour = """
                {
                  "name": "Bad Status Tour",
                  "location": "Hà Nội",
                  "registrationStartDate": "2026-11-01",
                  "registrationEndDate": "2026-11-16",
                  "status": "HACKED_STATE",
                  "startDate": "2026-11-20",
                  "endDate": "2026-11-30"
                }
                """;
        mockMvc.perform(post("/api/v1/tournaments")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidTour))
                .andExpect(status().isBadRequest());

        String invalidCompStatus = """
                {
                  "name": "Noi dung loi",
                  "tournamentId": %d,
                  "sportId": %d,
                  "status": "UNKNOWN_PHASE"
                }
                """.formatted(owned.getId(), sport.getId());
        mockMvc.perform(post("/api/v1/competitions")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidCompStatus))
                .andExpect(status().isBadRequest());
    }

    @Test
    void competitionOwnershipAndRuleFormatValidation_Enforced() throws Exception {
        String org1Token = token("organizer1", "ROLE_ORGANIZER");
        String org2Token = token("organizer2", "ROLE_ORGANIZER");
        String adminToken = token("admin", "ROLE_ADMIN");
        Tournament ownedByOrg1 = createTournamentEntity("Giai Cua Org1", "organizer1");

        // Missing sportId / non-existent tournamentId -> 400
        String badTournamentComp = """
                {
                  "name": "Bad Tour Ref",
                  "tournamentId": 999999,
                  "sportId": %d
                }
                """.formatted(sport.getId());
        mockMvc.perform(post("/api/v1/competitions")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badTournamentComp))
                .andExpect(status().isBadRequest());

        // org2 tries to add competition to org1 tournament -> 403
        String validComp = """
                {
                  "name": "Bong da Nam",
                  "tournamentId": %d,
                  "sportId": %d,
                  "status": "OPEN_REGISTRATION"
                }
                """.formatted(ownedByOrg1.getId(), sport.getId());
        mockMvc.perform(post("/api/v1/competitions")
                        .header("Authorization", org2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validComp))
                .andExpect(status().isForbidden());

        // org1 succeeds
        String compJson = mockMvc.perform(post("/api/v1/competitions")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validComp))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long compId = objectMapper.readTree(compJson).get("id").asLong();

        // Non-existent sportId on create -> 400
        String missingSportComp = """
                {
                  "name": "Bad Sport Ref",
                  "tournamentId": %d,
                  "sportId": 999999
                }
                """.formatted(ownedByOrg1.getId());
        mockMvc.perform(post("/api/v1/competitions")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingSportComp))
                .andExpect(status().isBadRequest());

        // org2 cannot edit rule/format -> 403
        mockMvc.perform(put("/api/v1/competitions/" + compId + "/rule")
                        .header("Authorization", org2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxParticipants\":16,\"minTeamMembers\":5,\"maxTeamMembers\":15}"))
                .andExpect(status().isForbidden());

        // org1 invalid rule (min > max, maxParticipants <= 0) -> 400
        mockMvc.perform(put("/api/v1/competitions/" + compId + "/rule")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxParticipants\":0,\"minTeamMembers\":10,\"maxTeamMembers\":5}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/competitions/" + compId + "/rule")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxParticipants\":16,\"minTeamMembers\":12,\"maxTeamMembers\":8}"))
                .andExpect(status().isBadRequest());

        // org1 valid rule -> 200
        mockMvc.perform(put("/api/v1/competitions/" + compId + "/rule")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxParticipants\":12,\"minTeamMembers\":5,\"maxTeamMembers\":15,\"scoringMethod\":\"POINTS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxParticipants", is(12)))
                .andExpect(jsonPath("$.minTeamMembers", is(5)));

        // org1 invalid format -> 400, valid format -> 200
        mockMvc.perform(put("/api/v1/competitions/" + compId + "/format")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formatType\":\"INVALID_MODE\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/competitions/" + compId + "/format")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formatType\":\"ROUND_ROBIN\",\"numberOfGroups\":1,\"qualifyingSpotsPerGroup\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formatType", is("ROUND_ROBIN")));

        // Updating format or rule after matches already exist -> 409 Conflict; match-service down -> 503
        matchMode.set("HAS_MATCH");
        mockMvc.perform(put("/api/v1/competitions/" + compId + "/format")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formatType\":\"KNOCKOUT\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/v1/competitions/" + compId + "/rule")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxParticipants\":16,\"minTeamMembers\":5,\"maxTeamMembers\":20}"))
                .andExpect(status().isConflict());

        matchMode.set("DOWN");
        mockMvc.perform(put("/api/v1/competitions/" + compId + "/format")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formatType\":\"KNOCKOUT\"}"))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(put("/api/v1/competitions/" + compId + "/rule")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxParticipants\":16,\"minTeamMembers\":5,\"maxTeamMembers\":20}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void deleteCompetitionAndDeleteTournament_CheckDependenciesAndServiceUnavailable() throws Exception {
        String org1Token = token("organizer1", "ROLE_ORGANIZER");
        Tournament tour = createTournamentEntity("Del Tour", "organizer1");

        String compJson = mockMvc.perform(post("/api/v1/competitions")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Del Comp",
                                  "tournamentId": %d,
                                  "sportId": %d
                                }
                                """.formatted(tour.getId(), sport.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long compId = objectMapper.readTree(compJson).get("id").asLong();

        // 1. Delete competition blocked when matches exist -> 409
        matchMode.set("HAS_MATCH");
        mockMvc.perform(delete("/api/v1/competitions/" + compId)
                        .header("Authorization", org1Token))
                .andExpect(status().isConflict());
        assertTrue(competitionRepository.existsById(compId));

        // 2. Delete competition blocked when registrations exist -> 409
        matchMode.set("HAS_REG");
        mockMvc.perform(delete("/api/v1/competitions/" + compId)
                        .header("Authorization", org1Token))
                .andExpect(status().isConflict());
        assertTrue(competitionRepository.existsById(compId));

        // 3. Delete competition blocked when match-service down -> 503
        matchMode.set("DOWN");
        mockMvc.perform(delete("/api/v1/competitions/" + compId)
                        .header("Authorization", org1Token))
                .andExpect(status().isServiceUnavailable());
        assertTrue(competitionRepository.existsById(compId));

        // 4. When empty -> deletes competition + orphan rule/format cleanly
        matchMode.set("EMPTY");
        mockMvc.perform(delete("/api/v1/competitions/" + compId)
                        .header("Authorization", org1Token))
                .andExpect(status().isNoContent());
        assertFalse(competitionRepository.existsById(compId));
        assertTrue(ruleRepository.findByCompetitionId(compId).isEmpty());
        assertTrue(formatRepository.findByCompetitionId(compId).isEmpty());

        // 5. Delete tournament fails with 503 when match-service unreachable (does NOT swallow exception)
        matchMode.set("DOWN");
        mockMvc.perform(delete("/api/v1/tournaments/" + tour.getId())
                        .header("Authorization", org1Token))
                .andExpect(status().isServiceUnavailable());
        assertTrue(tournamentRepository.existsById(tour.getId()));
    }

    @Test
    void tournamentDashboard_FiltersRankingsToOnlyTournamentCompetitions() throws Exception {
        String org1Token = token("organizer1", "ROLE_ORGANIZER");
        Tournament tourA = createTournamentEntity("Tour A", "organizer1");
        Tournament tourB = createTournamentEntity("Tour B", "organizer1");

        String compAResp = mockMvc.perform(post("/api/v1/competitions")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Comp A",
                                  "tournamentId": %d,
                                  "sportId": %d
                                }
                                """.formatted(tourA.getId(), sport.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long compAId = objectMapper.readTree(compAResp).get("id").asLong();

        String compBResp = mockMvc.perform(post("/api/v1/competitions")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Comp B",
                                  "tournamentId": %d,
                                  "sportId": %d
                                }
                                """.formatted(tourB.getId(), sport.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long compBId = objectMapper.readTree(compBResp).get("id").asLong();

        rankingsBody.set("""
                [
                  {"id":1,"competitionId":%d,"title":"BXH A"},
                  {"id":2,"competitionId":%d,"title":"BXH B1"},
                  {"id":3,"competitionId":%d,"title":"BXH B2"}
                ]
                """.formatted(compAId, compBId, compBId));

        String dashJson = mockMvc.perform(get("/api/v1/tournaments/" + tourA.getId() + "/dashboard"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(dashJson);
        assertEquals(1, root.get("rankingsCount").asInt());
        assertEquals(1, root.get("competitions").size());
    }

    @Test
    void sportsCatalog_allowsOrganizerCreate_restrictsPutDeleteToAdmin() throws Exception {
        String org1Token = token("organizer1", "ROLE_ORGANIZER");
        String adminToken = token("admin", "ROLE_ADMIN");

        // Organizer CAN POST a new sport
        String createdJson = mockMvc.perform(post("/api/v1/sports")
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Quần vợt C5\",\"sportType\":\"INDIVIDUAL\",\"description\":\"Môn đơn\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long sportId = objectMapper.readTree(createdJson).get("id").asLong();

        // Organizer CANNOT PUT or DELETE shared catalog sport -> 403 Forbidden
        mockMvc.perform(put("/api/v1/sports/" + sportId)
                        .header("Authorization", org1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked Sport\",\"sportType\":\"INDIVIDUAL\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/sports/" + sportId)
                        .header("Authorization", org1Token))
                .andExpect(status().isForbidden());

        // Admin CAN PUT & DELETE sport -> 200 & 204
        mockMvc.perform(put("/api/v1/sports/" + sportId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Quần vợt Quốc gia\",\"sportType\":\"INDIVIDUAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Quần vợt Quốc gia")));
        mockMvc.perform(delete("/api/v1/sports/" + sportId)
                        .header("Authorization", adminToken))
                .andExpect(status().isNoContent());
    }
}
