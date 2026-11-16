package com.sports.management.ranking;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sports.management.ranking.client.RankingProviderClient;
import com.sports.management.ranking.model.Ranking;
import com.sports.management.ranking.repository.RankingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ranking_sec_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class RankingSecurityIntegrationTest {

    private static final String JWT_SECRET = "SportsManagementSystemSuperSecretKeyWithAtLeast256BitsLongForHMACSHA256!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RankingRepository rankingRepository;

    @MockitoBean
    private RankingProviderClient rankingProviderClient;

    private Ranking savedRanking;

    @BeforeEach
    void setUp() {
        rankingRepository.deleteAll();
        Ranking r = new Ranking(null, 10L, "BXH Test", LocalDateTime.now());
        savedRanking = rankingRepository.save(r);
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
    void internalResultSyncScope_canTriggerRecalculateOnlyAndCannotOverwriteOrDeleteRankings() throws Exception {
        String internalSyncToken = issueToken("result-sync-worker", "ROLE_INTERNAL_RESULT_SYNC");

        when(rankingProviderClient.getMatchesByCompetitionId(10L)).thenReturn(List.of(
                Map.of("id", 1L, "competitionId", 10L, "participant1", "Hà Nội", "participant2", "Nam Định")
        ));
        when(rankingProviderClient.getAllResults()).thenReturn(List.of(
                Map.of("matchId", 1L, "status", "CONFIRMED", "scoreParticipant1", 2, "scoreParticipant2", 1)
        ));

        mockMvc.perform(post("/api/v1/rankings/competition/10/recalculate")
                        .header("Authorization", internalSyncToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].participantName").value("Hà Nội"));

        // Must be denied manual PUT items and DELETE
        mockMvc.perform(put("/api/v1/rankings/competition/10/items")
                        .header("Authorization", internalSyncToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/rankings/" + savedRanking.getId())
                        .header("Authorization", internalSyncToken))
                .andExpect(status().isForbidden());
    }
}
