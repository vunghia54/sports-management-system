package com.sports.management.athlete;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sports.management.athlete.model.Athlete;
import com.sports.management.athlete.repository.AthleteRepository;
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
import java.time.LocalDate;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AthletePrivacyValidationAndDeleteTest {

    private static final String JWT_SECRET = "SportsManagementSystemSuperSecretKeyWithAtLeast256BitsLongForHMACSHA256!";

    private static HttpServer mockServer;
    private static int mockPort;
    private static final AtomicReference<String> teamUsageMode = new AtomicReference<>("UNUSED"); // UNUSED, IN_TEAM, DOWN
    private static final AtomicReference<String> matchUsageMode = new AtomicReference<>("UNUSED"); // UNUSED, IN_USE, DOWN

    static {
        try {
            mockServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            mockPort = mockServer.getAddress().getPort();
            mockServer.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                int status = 200;
                String body = "{}";
                if (path.startsWith("/api/v1/teams/athletes/")) {
                    String mode = teamUsageMode.get();
                    if ("DOWN".equals(mode)) {
                        status = 500;
                        body = "{\"message\":\"Team down\"}";
                    } else {
                        body = "{\"athleteId\":1,\"inTeam\":" + "IN_TEAM".equals(mode) + "}";
                    }
                } else if (path.startsWith("/api/v1/participants/usage")) {
                    String mode = matchUsageMode.get();
                    if ("DOWN".equals(mode)) {
                        status = 500;
                        body = "{\"message\":\"Match down\"}";
                    } else {
                        boolean used = "IN_USE".equals(mode);
                        body = "{\"hasActiveRegistrations\":" + used + ",\"hasParticipants\":" + used + ",\"inUse\":" + used + "}";
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
        registry.add("services.team.url", () -> baseUrl);
        registry.add("services.match.url", () -> baseUrl);
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:b3athletedb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
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
    private AthleteRepository athleteRepository;

    @BeforeEach
    void setUp() {
        teamUsageMode.set("UNUSED");
        matchUsageMode.set("UNUSED");
        athleteRepository.deleteAll();
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

    private Athlete createAthleteEntity(String name, String cccd, String owner) {
        Athlete a = new Athlete();
        a.setFullName(name);
        a.setDateOfBirth(LocalDate.of(1998, 5, 10));
        a.setGender("MALE");
        a.setIdentityCardNumber(cccd);
        a.setPhone("0901234567");
        a.setEmail("vdv@sports.vn");
        a.setHealthStatus("QUALIFIED");
        a.setOwnerUsername(owner);
        return athleteRepository.save(a);
    }

    @Test
    void piiRedactionAndCccdSearchAuthZ_EnforcedAcrossRoles() throws Exception {
        Athlete a = createAthleteEntity("Nguyen Van Secret", "001098999111", "manager1");

        String viewerToken = token("viewer1", "ROLE_VIEWER");
        String mgr1Token = token("manager1", "ROLE_TEAM_MANAGER");
        String mgr2Token = token("manager2", "ROLE_TEAM_MANAGER");
        String orgToken = token("organizer1", "ROLE_ORGANIZER");

        // 1. Anonymous GET by id -> PII nulled out
        mockMvc.perform(get("/api/v1/athletes/" + a.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Nguyen Van Secret")))
                .andExpect(jsonPath("$.identityCardNumber").value(nullValue()))
                .andExpect(jsonPath("$.phone").value(nullValue()))
                .andExpect(jsonPath("$.email").value(nullValue()))
                .andExpect(jsonPath("$.dateOfBirth").value(nullValue()));

        // 2. Other team manager (manager2) -> STILL redacted + cannot probe CCCD via ?keyword=
        mockMvc.perform(get("/api/v1/athletes/" + a.getId())
                        .header("Authorization", mgr2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identityCardNumber").value(nullValue()))
                .andExpect(jsonPath("$.phone").value(nullValue()));
        mockMvc.perform(get("/api/v1/athletes?keyword=001098999111")
                        .header("Authorization", mgr2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));

        // 3. Owner (manager1) and Organizer get full PII
        mockMvc.perform(get("/api/v1/athletes/" + a.getId())
                        .header("Authorization", mgr1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identityCardNumber", is("001098999111")))
                .andExpect(jsonPath("$.phone", is("0901234567")))
                .andExpect(jsonPath("$.email", is("vdv@sports.vn")))
                .andExpect(jsonPath("$.dateOfBirth", is("1998-05-10")));

        mockMvc.perform(get("/api/v1/athletes/" + a.getId())
                        .header("Authorization", orgToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identityCardNumber", is("001098999111")));

        // 4. Dedicated CCCD search endpoint authZ
        mockMvc.perform(get("/api/v1/athletes/cccd/001098999111"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/athletes/cccd/001098999111")
                        .header("Authorization", viewerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/athletes/cccd/001098999111")
                        .header("Authorization", mgr2Token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/athletes/cccd/001098999111")
                        .header("Authorization", mgr1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identityCardNumber", is("001098999111")));
        mockMvc.perform(get("/api/v1/athletes/cccd/001098999111")
                        .header("Authorization", orgToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Nguyen Van Secret")));
    }

    @Test
    void uniqueCccdAndFieldValidation_RejectsDuplicatesFutureDobAndInvalidEnums() throws Exception {
        String mgr1Token = token("manager1", "ROLE_TEAM_MANAGER");

        // Future DOB -> 400
        String futureDob = LocalDate.now().plusYears(1).toString();
        mockMvc.perform(post("/api/v1/athletes")
                        .header("Authorization", mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Future Kid","dateOfBirth":"%s","gender":"MALE","healthStatus":"QUALIFIED"}
                                """.formatted(futureDob)))
                .andExpect(status().isBadRequest());

        // Invalid gender -> 400
        mockMvc.perform(post("/api/v1/athletes")
                        .header("Authorization", mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Bad Gender","dateOfBirth":"1998-01-01","gender":"ALIEN","healthStatus":"QUALIFIED"}
                                """))
                .andExpect(status().isBadRequest());

        // Invalid / legacy FIT healthStatus -> 400 (only QUALIFIED, INJURED, SUSPENDED allowed)
        mockMvc.perform(post("/api/v1/athletes")
                        .header("Authorization", mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Legacy Fit","dateOfBirth":"1998-01-01","gender":"MALE","healthStatus":"FIT"}
                                """))
                .andExpect(status().isBadRequest());

        // Create valid athlete with CCCD
        mockMvc.perform(post("/api/v1/athletes")
                        .header("Authorization", mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"VDV Hop Le","dateOfBirth":"1999-03-04","gender":"FEMALE","identityCardNumber":"001099111222","healthStatus":"QUALIFIED"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identityCardNumber", is("001099111222")));

        // Duplicate CCCD -> 409 Conflict
        mockMvc.perform(post("/api/v1/athletes")
                        .header("Authorization", mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"VDV Trung CCCD","dateOfBirth":"1999-03-04","gender":"MALE","identityCardNumber":"001099111222","healthStatus":"QUALIFIED"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteAthlete_BlocksWhenInTeamOrMatch_And503WhenServiceDown() throws Exception {
        String adminToken = token("admin", "ROLE_ADMIN");
        Athlete a = createAthleteEntity("VDV Delete Test", "001099888777", "manager1");

        // 1. In team -> 409
        teamUsageMode.set("IN_TEAM");
        mockMvc.perform(delete("/api/v1/athletes/" + a.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isConflict());
        assertTrue(athleteRepository.existsById(a.getId()));

        // 2. Team service down -> 503
        teamUsageMode.set("DOWN");
        mockMvc.perform(delete("/api/v1/athletes/" + a.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isServiceUnavailable());
        assertTrue(athleteRepository.existsById(a.getId()));

        // 3. Not in team, but active in match-service -> 409
        teamUsageMode.set("UNUSED");
        matchUsageMode.set("IN_USE");
        mockMvc.perform(delete("/api/v1/athletes/" + a.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isConflict());
        assertTrue(athleteRepository.existsById(a.getId()));

        // 4. Match service down -> 503
        matchUsageMode.set("DOWN");
        mockMvc.perform(delete("/api/v1/athletes/" + a.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isServiceUnavailable());
        assertTrue(athleteRepository.existsById(a.getId()));

        // 5. Unused everywhere -> 204
        teamUsageMode.set("UNUSED");
        matchUsageMode.set("UNUSED");
        mockMvc.perform(delete("/api/v1/athletes/" + a.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isNoContent());
        assertFalse(athleteRepository.existsById(a.getId()));
    }

    @Test
    void actuatorPrometheusIsPublic_whileActuatorEnvRemainsSecured() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("application=\"athlete-service\"")));

        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }
}
