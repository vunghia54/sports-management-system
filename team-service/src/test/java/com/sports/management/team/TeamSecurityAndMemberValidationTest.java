package com.sports.management.team;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sports.management.team.model.Team;
import com.sports.management.team.model.TeamMember;
import com.sports.management.team.repository.TeamMemberRepository;
import com.sports.management.team.repository.TeamRepository;
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
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TeamSecurityAndMemberValidationTest {

    private static final String JWT_SECRET = "SportsManagementSystemSuperSecretKeyWithAtLeast256BitsLongForHMACSHA256!";

    private static HttpServer mockServer;
    private static int mockPort;
    private static final AtomicReference<String> athleteHealth = new AtomicReference<>("QUALIFIED");
    private static final AtomicReference<String> matchUsageMode = new AtomicReference<>("UNUSED"); // UNUSED, IN_USE, DOWN

    static {
        try {
            mockServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            mockPort = mockServer.getAddress().getPort();
            mockServer.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                int status = 200;
                String body = "{}";
                if (path.startsWith("/api/v1/athletes/")) {
                    String idStr = path.substring("/api/v1/athletes/".length());
                    if ("9999".equals(idStr)) {
                        status = 404;
                        body = "{\"message\":\"Not found\"}";
                    } else {
                        body = """
                                {"id":%s,"fullName":"VDV Test %s","healthStatus":"%s"}
                                """.formatted(idStr, idStr, athleteHealth.get());
                    }
                } else if (path.startsWith("/api/v1/participants/usage")) {
                    String mode = matchUsageMode.get();
                    if ("DOWN".equals(mode)) {
                        status = 500;
                        body = "{\"message\":\"Match down\"}";
                    } else if ("IN_USE".equals(mode)) {
                        body = "{\"hasActiveRegistrations\":true,\"hasParticipants\":true,\"inUse\":true}";
                    } else {
                        body = "{\"hasActiveRegistrations\":false,\"hasParticipants\":false,\"inUse\":false}";
                    }
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
    static void registerProps(DynamicPropertyRegistry registry) {
        String baseUrl = "http://127.0.0.1:" + mockPort;
        registry.add("services.athlete.url", () -> baseUrl);
        registry.add("services.match.url", () -> baseUrl);
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:b2teamdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
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
    private TeamRepository teamRepository;
    @Autowired
    private TeamMemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        athleteHealth.set("QUALIFIED");
        matchUsageMode.set("UNUSED");
        memberRepository.deleteAll();
        teamRepository.deleteAll();
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

    private Team createTeamEntity(String name, String owner) {
        Team t = new Team();
        t.setName(name);
        t.setShortName(name.substring(0, Math.min(4, name.length())).toUpperCase());
        t.setManagerName("HLV");
        t.setOwnerUsername(owner);
        return teamRepository.save(t);
    }

    @Test
    void removeMember_ChecksMemberBelongsToTeamId_AndRejectsCrossTeamRemovalWith404() throws Exception {
        String mgr1 = token("manager1", "ROLE_TEAM_MANAGER");
        Team teamA = createTeamEntity("Team Alpha", "manager1");
        Team teamB = createTeamEntity("Team Beta", "manager2");

        TeamMember memberInB = new TeamMember();
        memberInB.setTeamId(teamB.getId());
        memberInB.setAthleteId(501L);
        memberInB.setAthleteName("VDV Beta");
        memberInB.setJerseyNumber(7);
        memberInB = memberRepository.save(memberInB);

        // manager1 owns teamA, tries deleting memberInB via /teams/{teamA}/members/{memberInB}
        mockMvc.perform(delete("/api/v1/teams/" + teamA.getId() + "/members/" + memberInB.getId())
                        .header("Authorization", mgr1))
                .andExpect(status().isNotFound());
        assertTrue(memberRepository.existsById(memberInB.getId()));
    }

    @Test
    void addMember_EnforcesAthleteIdQualifiedHealthDuplicateAthleteAndJerseyNumber() throws Exception {
        String mgr1 = token("manager1", "ROLE_TEAM_MANAGER");
        Team team = createTeamEntity("CLB Ha Noi B2", "manager1");

        // 1. Missing athleteId -> 400
        mockMvc.perform(post("/api/v1/teams/" + team.getId() + "/members")
                        .header("Authorization", mgr1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jerseyNumber\":10}"))
                .andExpect(status().isBadRequest());

        // 2. Athlete not QUALIFIED -> 400
        athleteHealth.set("INJURED");
        mockMvc.perform(post("/api/v1/teams/" + team.getId() + "/members")
                        .header("Authorization", mgr1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"athleteId\":101,\"jerseyNumber\":10}"))
                .andExpect(status().isBadRequest());

        // 3. Qualified athlete succeeds & auto-fills athleteName
        athleteHealth.set("QUALIFIED");
        mockMvc.perform(post("/api/v1/teams/" + team.getId() + "/members")
                        .header("Authorization", mgr1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"athleteId\":101,\"jerseyNumber\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.athleteName", is("VDV Test 101")));

        // 4. Duplicate athlete in same team -> 409 Conflict
        mockMvc.perform(post("/api/v1/teams/" + team.getId() + "/members")
                        .header("Authorization", mgr1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"athleteId\":101,\"jerseyNumber\":11}"))
                .andExpect(status().isConflict());

        // 5. Duplicate jersey number in same team -> 409 Conflict
        mockMvc.perform(post("/api/v1/teams/" + team.getId() + "/members")
                        .header("Authorization", mgr1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"athleteId\":102,\"jerseyNumber\":10}"))
                .andExpect(status().isConflict());
    }

    @Test
    void ownershipAlignmentAndPatchValidationAndSortAllowlist_Enforced() throws Exception {
        String mgr1 = token("manager1", "ROLE_TEAM_MANAGER");
        String org1 = token("organizer1", "ROLE_ORGANIZER");
        String admin = token("admin", "ROLE_ADMIN");

        // Organizer is blocked by SecurityConfig & ownership -> 403
        Team ownedByMgr1 = createTeamEntity("CLB Manager 1", "manager1");
        mockMvc.perform(put("/api/v1/teams/" + ownedByMgr1.getId())
                        .header("Authorization", org1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked\"}"))
                .andExpect(status().isForbidden());

        // Null ownerUsername team is strictly Admin-only -> manager1 gets 403
        Team legacy = createTeamEntity("CLB Legacy Null Owner", null);
        mockMvc.perform(patch("/api/v1/teams/" + legacy.getId())
                        .header("Authorization", mgr1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shortName\":\"LEG\"}"))
                .andExpect(status().isForbidden());

        // Spoofing ownerUsername on createTeam -> 403
        mockMvc.perform(post("/api/v1/teams")
                        .header("Authorization", mgr1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Spoof Team\",\"ownerUsername\":\"manager2\"}"))
                .andExpect(status().isForbidden());

        // patchTeam with non-string field -> 400 (not 500)
        mockMvc.perform(patch("/api/v1/teams/" + ownedByMgr1.getId())
                        .header("Authorization", mgr1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":12345}"))
                .andExpect(status().isBadRequest());

        // Invalid sortBy / direction -> 400
        mockMvc.perform(get("/api/v1/teams?sortBy=nonExistentField"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/teams/paged?sortBy=id&direction=diagonal"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/teams/paged?sortBy=name&direction=desc"))
                .andExpect(status().isOk());

        // Admin deleteTeam checks match-service usage: IN_USE -> 409, DOWN -> 503, UNUSED -> 204
        matchUsageMode.set("IN_USE");
        mockMvc.perform(delete("/api/v1/teams/" + ownedByMgr1.getId())
                        .header("Authorization", admin))
                .andExpect(status().isConflict());
        assertTrue(teamRepository.existsById(ownedByMgr1.getId()));

        matchUsageMode.set("DOWN");
        mockMvc.perform(delete("/api/v1/teams/" + ownedByMgr1.getId())
                        .header("Authorization", admin))
                .andExpect(status().isServiceUnavailable());
        assertTrue(teamRepository.existsById(ownedByMgr1.getId()));

        matchUsageMode.set("UNUSED");
        mockMvc.perform(delete("/api/v1/teams/" + ownedByMgr1.getId())
                        .header("Authorization", admin))
                .andExpect(status().isNoContent());
        assertFalse(teamRepository.existsById(ownedByMgr1.getId()));
    }
}
