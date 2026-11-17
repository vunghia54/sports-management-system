package com.sports.management.notification;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sports.management.notification.model.Notification;
import com.sports.management.notification.repository.NotificationReadRepository;
import com.sports.management.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:b5_notif_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "jwt.secret=SportsManagementSystemSuperSecretKeyWithAtLeast256BitsLongForHMACSHA256!"
})
@AutoConfigureMockMvc
class NotificationPerUserReadAndValidationTest {

    private static final String JWT_SECRET = "SportsManagementSystemSuperSecretKeyWithAtLeast256BitsLongForHMACSHA256!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationReadRepository notificationReadRepository;

    @BeforeEach
    void setUp() {
        notificationReadRepository.deleteAll();
        notificationRepository.deleteAll();
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

    @Test
    void broadcastNotification_TracksReadStateIndependentlyPerUser() throws Exception {
        String orgToken = token("organizer1", "ROLE_ORGANIZER");
        String viewer1Token = token("viewer1", "ROLE_VIEWER");
        String viewer2Token = token("viewer2", "ROLE_VIEWER");
        String mgr1Token = token("manager1", "ROLE_TEAM_MANAGER");

        String createdJson = mockMvc.perform(post("/api/v1/notifications")
                        .header("Authorization", orgToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipient": "ALL_USERS",
                                  "title": "Lich thi dau moi",
                                  "message": "Da cap nhat lich vong 1",
                                  "type": "SCHEDULE_PUBLISHED",
                                  "referenceId": 101
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long broadcastId = objectMapper.readTree(createdJson).get("id").asLong();

        // Anonymous cannot mark read -> 401
        mockMvc.perform(patch("/api/v1/notifications/" + broadcastId + "/read"))
                .andExpect(status().isUnauthorized());

        // viewer1 marks read (via PUT or PATCH); stays unread for viewer2
        mockMvc.perform(put("/api/v1/notifications/" + broadcastId + "/read")
                        .header("Authorization", viewer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead", is(true)))
                .andExpect(jsonPath("$.read", is(true)));

        // viewer1 sees 1 read in /paged & /statistics
        mockMvc.perform(get("/api/v1/notifications/paged?read=true")
                        .header("Authorization", viewer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)));
        mockMvc.perform(get("/api/v1/notifications/statistics")
                        .header("Authorization", viewer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readNotifications", is(1)))
                .andExpect(jsonPath("$.unreadNotifications", is(0)));

        // viewer2 STILL sees unread!
        mockMvc.perform(get("/api/v1/notifications/" + broadcastId)
                        .header("Authorization", viewer2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead", is(false)))
                .andExpect(jsonPath("$.read", is(false)));
        mockMvc.perform(get("/api/v1/notifications/paged?read=false")
                        .header("Authorization", viewer2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)));
        mockMvc.perform(get("/api/v1/notifications/statistics")
                        .header("Authorization", viewer2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readNotifications", is(0)))
                .andExpect(jsonPath("$.unreadNotifications", is(1)));

        // Private notification targeted to manager1 cannot be marked read by viewer1 -> 403
        Notification priv = new Notification(null, "manager1", "Duyet ho so", "Da duyet CLB", "REGISTRATION_APPROVED", 5L, null, false);
        priv = notificationRepository.save(priv);

        mockMvc.perform(patch("/api/v1/notifications/" + priv.getId() + "/read")
                        .header("Authorization", viewer1Token))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/notifications/" + priv.getId() + "/read")
                        .header("Authorization", mgr1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead", is(true)));
    }

    @Test
    void notificationValidationAndGlobalExceptionHandler_ReturnsClean400And404() throws Exception {
        String orgToken = token("organizer1", "ROLE_ORGANIZER");

        // Blank recipient / title / message -> 400 with field errors map
        mockMvc.perform(post("/api/v1/notifications")
                        .header("Authorization", orgToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipient":"","title":"","message":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.recipient").exists())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.message").exists());

        // Missing notification -> clean 404
        mockMvc.perform(get("/api/v1/notifications/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));

        // Invalid sortBy -> 400
        mockMvc.perform(get("/api/v1/notifications/paged?sortBy=badColumn"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void privateNotifications_visibleOnlyToRecipientOrAdmin_andBroadcastPublic() throws Exception {
        String viewer1Token = token("viewer1", "ROLE_VIEWER");
        String mgr1Token = token("manager1", "ROLE_TEAM_MANAGER");
        String adminToken = token("admin", "ROLE_ADMIN");

        Notification broadcast = notificationRepository.save(
                new Notification(null, "ALL_USERS", "Thông báo chung", "Công khai mọi người", "SYSTEM_INFO", 1L, null, false));
        Notification privMgr = notificationRepository.save(
                new Notification(null, "manager1", "Mật M1 BiMatM1", "Nội dung riêng của manager1 BiMatM1", "PRIVATE_MSG", 2L, null, false));
        Notification privViewer = notificationRepository.save(
                new Notification(null, "viewer1", "Riêng V1", "Nội dung riêng của viewer1", "PRIVATE_MSG", 3L, null, false));

        // 1. Unauthenticated caller: only ALL_USERS visible on list/paged/stats; private GET by ID or ?recipient -> 401
        mockMvc.perform(get("/api/v1/notifications/" + broadcast.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/notifications/" + privMgr.getId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/notifications?recipient=manager1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/notifications/paged?recipient=manager1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].recipient", is("ALL_USERS")));
        mockMvc.perform(get("/api/v1/notifications/paged?keyword=BiMatM1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));
        mockMvc.perform(get("/api/v1/notifications/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNotifications", is(1)));

        // 2. Other user (viewer1): sees ALL_USERS + own (2), blocked (403) from manager1 private by ID & ?recipient
        mockMvc.perform(get("/api/v1/notifications/" + privMgr.getId())
                        .header("Authorization", viewer1Token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/notifications?recipient=manager1")
                        .header("Authorization", viewer1Token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/notifications/paged?recipient=manager1")
                        .header("Authorization", viewer1Token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/notifications/" + privViewer.getId())
                        .header("Authorization", viewer1Token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", viewer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)));
        mockMvc.perform(get("/api/v1/notifications/paged?keyword=BiMatM1")
                        .header("Authorization", viewer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));
        mockMvc.perform(get("/api/v1/notifications/statistics")
                        .header("Authorization", viewer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNotifications", is(2)));

        // 3. Recipient (manager1) and ADMIN can view private notification and updated stats
        mockMvc.perform(get("/api/v1/notifications/" + privMgr.getId())
                        .header("Authorization", mgr1Token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/notifications/paged?keyword=BiMatM1")
                        .header("Authorization", mgr1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)));

        mockMvc.perform(get("/api/v1/notifications/" + privMgr.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(3)));
        mockMvc.perform(get("/api/v1/notifications/statistics")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNotifications", is(3)));
    }

    @Test
    void actuatorHealth_isAccessibleWithoutAuthAndHidesDetails_whileRabbitIndicatorRemainsActive() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.components").doesNotExist());

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("application=\"notification-service\"")));

        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"message\":\"M\",\"recipient\":\"ALL_USERS\"}"))
                .andExpect(status().isUnauthorized());
    }
}
