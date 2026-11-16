package com.sports.management.auth;

import com.sports.management.auth.model.User;
import com.sports.management.auth.repository.UserRepository;
import com.sports.management.auth.util.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth_pw_sec_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "app.demo.seed-users=false",
        "jwt.secret=SportsManagementSystemSuperSecretKeyWithAtLeast256BitsLongForHMACSHA256!"
})
@AutoConfigureMockMvc
class AuthPasswordSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User adminUser;
    private User viewerUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        adminUser = userRepository.save(new User(
                null, "admin", passwordEncoder.encode("admin123"), "Admin", "admin@vn", "ROLE_ADMIN", true
        ));
        viewerUser = userRepository.save(new User(
                null, "viewer1", passwordEncoder.encode("viewer123"), "Viewer", "v1@vn", "ROLE_VIEWER", true
        ));
    }

    @Test
    void changePassword_withoutToken_returns401() throws Exception {
        mockMvc.perform(put("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"currentPassword\":\"admin123\",\"newPassword\":\"hacked123\"}"))
                .andExpect(status().isUnauthorized());

        User reloadedAdmin = userRepository.findByUsername("admin").orElseThrow();
        assertTrue(passwordEncoder.matches("admin123", reloadedAdmin.getPassword()));
    }

    @Test
    void changePassword_wrongOrMissingCurrentPassword_returns400() throws Exception {
        String viewerToken = "Bearer " + jwtTokenProvider.generateToken(viewerUser.getUsername(), viewerUser.getId(), viewerUser.getRole());

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .header("Authorization", viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"\",\"newPassword\":\"newpass123\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .header("Authorization", viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrongpass\",\"newPassword\":\"newpass123\"}"))
                .andExpect(status().isBadRequest());

        User reloadedViewer = userRepository.findByUsername("viewer1").orElseThrow();
        assertTrue(passwordEncoder.matches("viewer123", reloadedViewer.getPassword()));
    }

    @Test
    void changePassword_cannotChangeAnotherUsersPassword_andChangesSelfWhenValid() throws Exception {
        String viewerToken = "Bearer " + jwtTokenProvider.generateToken(viewerUser.getUsername(), viewerUser.getId(), viewerUser.getRole());

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .header("Authorization", viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"currentPassword\":\"viewer123\",\"newPassword\":\"hacked123\"}"))
                .andExpect(status().isForbidden());

        User reloadedAdmin = userRepository.findByUsername("admin").orElseThrow();
        assertTrue(passwordEncoder.matches("admin123", reloadedAdmin.getPassword()));

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .header("Authorization", viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"viewer123\",\"newPassword\":\"viewer456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("viewer1"));

        User reloadedViewer = userRepository.findByUsername("viewer1").orElseThrow();
        assertTrue(passwordEncoder.matches("viewer456", reloadedViewer.getPassword()));
        assertFalse(passwordEncoder.matches("viewer123", reloadedViewer.getPassword()));
    }

    @Test
    void adminResetPassword_requiresAdminAndUpdatesTargetUser() throws Exception {
        String viewerToken = "Bearer " + jwtTokenProvider.generateToken(viewerUser.getUsername(), viewerUser.getId(), viewerUser.getRole());
        String adminToken = "Bearer " + jwtTokenProvider.generateToken(adminUser.getUsername(), adminUser.getId(), adminUser.getRole());

        mockMvc.perform(put("/api/v1/auth/users/" + viewerUser.getId() + "/password")
                        .header("Authorization", viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"reset123\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/auth/users/" + viewerUser.getId() + "/password")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"reset999\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("viewer1"));

        User reloadedViewer = userRepository.findByUsername("viewer1").orElseThrow();
        assertTrue(passwordEncoder.matches("reset999", reloadedViewer.getPassword()));
    }
}
