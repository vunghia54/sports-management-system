package com.sports.management.auth;

import com.sports.management.auth.controller.AuthController;
import com.sports.management.auth.exception.GlobalExceptionHandler;
import com.sports.management.auth.model.User;
import com.sports.management.auth.repository.UserRepository;
import com.sports.management.auth.service.AuthService;
import com.sports.management.auth.util.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerPagedUsersTest {

    @Mock
    private AuthService authService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authService, jwtTokenProvider, userRepository, "auth-service:8088");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getUsersPaged_rejectsMissingOrExpiredTokenWith401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/users/paged"))
                .andExpect(status().isUnauthorized());

        when(jwtTokenProvider.validateToken("expired-jwt")).thenReturn(false);
        mockMvc.perform(get("/api/v1/auth/users/paged").header("Authorization", "Bearer expired-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUsersPaged_rejectsNonAdminWith403() throws Exception {
        when(jwtTokenProvider.validateToken("org-jwt")).thenReturn(true);
        when(jwtTokenProvider.getRoleFromToken("org-jwt")).thenReturn("ROLE_ORGANIZER");

        mockMvc.perform(get("/api/v1/auth/users/paged").header("Authorization", "Bearer org-jwt"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUsersPaged_filtersAndPaginatesForAdmin() throws Exception {
        when(jwtTokenProvider.validateToken("admin-jwt")).thenReturn(true);
        when(jwtTokenProvider.getRoleFromToken("admin-jwt")).thenReturn("ROLE_ADMIN");

        User u1 = new User(1L, "admin", "h", "Quản trị viên", "admin@vn", "ROLE_ADMIN", true);
        User u2 = new User(2L, "referee1", "h", "Trọng tài 1", "ref1@vn", "ROLE_REFEREE", true);
        User u3 = new User(3L, "referee2", "h", "Trọng tài 2", "ref2@vn", "ROLE_REFEREE", true);
        when(userRepository.findAll()).thenReturn(List.of(u1, u2, u3));

        mockMvc.perform(get("/api/v1/auth/users/paged")
                        .header("Authorization", "Bearer admin-jwt")
                        .param("role", "ROLE_REFEREE")
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].username").value("referee2"));
    }
}
