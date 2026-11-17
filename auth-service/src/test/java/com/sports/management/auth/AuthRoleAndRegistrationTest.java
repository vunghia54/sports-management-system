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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth_role_reg_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class AuthRoleAndRegistrationTest {

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
    void register_allowsViewerAndTeamManager_andRejectsPrivilegedOrInternalRolesWith400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new_viewer\",\"password\":\"secret123\",\"fullName\":\"Viewer\",\"email\":\"v@vn\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ROLE_VIEWER"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new_mgr\",\"password\":\"secret123\",\"fullName\":\"Mgr\",\"email\":\"m@vn\",\"role\":\"ROLE_TEAM_MANAGER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ROLE_TEAM_MANAGER"));

        for (String forbiddenRole : new String[]{"ROLE_ORGANIZER", "ROLE_REFEREE", "ROLE_ADMIN", "ROLE_INTERNAL_RESULT_SYNC", "ROLE_USER", "INVALID_ROLE"}) {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"bad_" + forbiddenRole + "\",\"password\":\"secret123\",\"role\":\"" + forbiddenRole + "\"}"))
                    .andExpect(status().isBadRequest());
            assertFalse(userRepository.existsByUsername("bad_" + forbiddenRole));
        }
    }

    @Test
    void register_rejectsPasswordShorterThan6() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"shortpw\",\"password\":\"12345\",\"role\":\"ROLE_VIEWER\"}"))
                .andExpect(status().isBadRequest());
        assertFalse(userRepository.existsByUsername("shortpw"));
    }

    @Test
    void updateUserRole_allowsOnlyFiveWhitelistRoles_rejectsInternalOrUnknown() throws Exception {
        String adminToken = "Bearer " + jwtTokenProvider.generateToken(adminUser.getUsername(), adminUser.getId(), adminUser.getRole());

        mockMvc.perform(put("/api/v1/auth/users/" + viewerUser.getId() + "/role")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ROLE_ORGANIZER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_ORGANIZER"));

        mockMvc.perform(put("/api/v1/auth/users/" + viewerUser.getId() + "/role")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ROLE_REFEREE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_REFEREE"));

        for (String disallowed : new String[]{"ROLE_INTERNAL_RESULT_SYNC", "INTERNAL_RESULT_SYNC", "ROLE_SUPERUSER", ""}) {
            mockMvc.perform(put("/api/v1/auth/users/" + viewerUser.getId() + "/role")
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"role\":\"" + disallowed + "\"}"))
                    .andExpect(status().isBadRequest());
        }
        assertEquals("ROLE_REFEREE", userRepository.findById(viewerUser.getId()).orElseThrow().getRole());
    }

    @Test
    void adminCannotSelfLockSelfDeleteSelfDemoteOrRemoveLastAdmin() throws Exception {
        String adminToken = "Bearer " + jwtTokenProvider.generateToken(adminUser.getUsername(), adminUser.getId(), adminUser.getRole());

        mockMvc.perform(put("/api/v1/auth/users/" + adminUser.getId() + "/role")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ROLE_VIEWER\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/v1/auth/users/" + adminUser.getId() + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/v1/auth/users/" + adminUser.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isBadRequest());

        // Second admin promotes viewer -> still cannot remove LAST active admin via another admin
        User secondAdmin = userRepository.save(new User(
                null, "admin2", passwordEncoder.encode("admin123"), "Admin2", "a2@vn", "ROLE_ADMIN", true
        ));
        String admin2Token = "Bearer " + jwtTokenProvider.generateToken(secondAdmin.getUsername(), secondAdmin.getId(), secondAdmin.getRole());
        // admin2 demotes admin -> allowed since 2 admins exist
        mockMvc.perform(put("/api/v1/auth/users/" + adminUser.getId() + "/role")
                        .header("Authorization", admin2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ROLE_ORGANIZER\"}"))
                .andExpect(status().isOk());
        // admin2 now is last admin and still cannot self-demote/self-lock/self-delete
        mockMvc.perform(delete("/api/v1/auth/users/" + secondAdmin.getId())
                        .header("Authorization", admin2Token))
                .andExpect(status().isBadRequest());
        assertTrue(userRepository.existsById(secondAdmin.getId()));
    }

    @Test
    void registerAndLogin_enforceNormalizedUsernameUniqueness_andReportDuplicatesWithoutAutoMerging() throws Exception {
        // 1. Registering uppercase or whitespace variant of existing "viewer1" must fail with 400
        for (String variant : new String[]{"VIEWER1", "Viewer1", "  viewer1  "}) {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"" + variant + "\",\"password\":\"secret123\",\"role\":\"ROLE_TEAM_MANAGER\"}"))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(2, userRepository.count());

        // 2. Registering with mixed case + whitespace stores canonical lowercase trimmed username
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"  ManagerUpper  \",\"password\":\"secret123\",\"role\":\"ROLE_TEAM_MANAGER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("managerupper"));
        assertTrue(userRepository.existsByUsername("managerupper"));

        // 3. Login with mixed case + whitespace resolves same canonical identity & JWT subject
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"  MANAGERUPPER  \",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("managerupper"));

        // 4. Legacy DB containing two duplicate variants after normalization -> reports 409 Conflict without deleting or merging
        userRepository.save(new User(null, "DupLegacy", passwordEncoder.encode("secret123"), "D1", "d1@vn", "ROLE_VIEWER", true));
        userRepository.save(new User(null, "duplegacy", passwordEncoder.encode("secret123"), "D2", "d2@vn", "ROLE_VIEWER", true));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"DupLegacy\",\"password\":\"secret123\"}"))
                .andExpect(status().isConflict());
        assertEquals(2, userRepository.findAllByNormalizedUsername("duplegacy").size());
    }
}
