package com.sports.management.auth.service;

import com.sports.management.auth.dto.LoginRequest;
import com.sports.management.auth.dto.LoginResponse;
import com.sports.management.auth.dto.RegisterRequest;
import com.sports.management.auth.model.User;
import com.sports.management.auth.repository.UserRepository;
import com.sports.management.auth.util.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

@Service
public class AuthService {

    private static final Set<String> SELF_REGISTER_ROLES = Set.of("ROLE_VIEWER", "ROLE_TEAM_MANAGER");
    private static final Set<String> ASSIGNABLE_ROLES = Set.of(
            "ROLE_ADMIN", "ROLE_ORGANIZER", "ROLE_REFEREE", "ROLE_TEAM_MANAGER", "ROLE_VIEWER"
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid username or password"
                ));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid username or password"
            );
        }

        if (!user.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Account is disabled"
            );
        }

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getId(), user.getRole());
        var permissions = com.sports.management.auth.model.RolePermissions.getPermissionsForRole(user.getRole());

        return new LoginResponse(
                token,
                "Bearer",
                jwtTokenProvider.getJwtExpirationSeconds(),
                user.getId(),
                user.getUsername(),
                user.getRole(),
                permissions
        );
    }

    public User register(RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu đăng ký phải có ít nhất 6 ký tự");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Username already exists"
            );
        }

        String normalizedRole = normalizeRole(request.getRole());
        if (normalizedRole == null) {
            normalizedRole = "ROLE_VIEWER";
        }
        if (!SELF_REGISTER_ROLES.contains(normalizedRole)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đăng ký công khai chỉ chấp nhận ROLE_VIEWER hoặc ROLE_TEAM_MANAGER"
            );
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setRole(normalizedRole);
        user.setActive(true);

        return userRepository.save(user);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found with username: " + username
                ));
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = getUserByUsername(username);
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập mật khẩu hiện tại");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không chính xác");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới phải có ít nhất 6 ký tự");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public User adminResetPassword(Long id, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới phải có ít nhất 6 ký tự");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng ID: " + id));
        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    public User updateUserRole(Long id, String role) {
        return updateUserRole(id, role, null);
    }

    public User updateUserRole(Long id, String role, String actorUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng ID: " + id));
        String normalized = normalizeRole(role);
        if (normalized == null || !ASSIGNABLE_ROLES.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vai trò không hợp lệ. Chỉ chấp nhận: ADMIN, ORGANIZER, REFEREE, TEAM_MANAGER, VIEWER"
            );
        }
        boolean currentlyAdmin = "ROLE_ADMIN".equalsIgnoreCase(user.getRole());
        if (currentlyAdmin && !"ROLE_ADMIN".equals(normalized)) {
            if (actorUsername != null && actorUsername.equalsIgnoreCase(user.getUsername())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quản trị viên không thể tự hạ quyền chính mình");
            }
            if (user.isActive() && userRepository.countByRoleIgnoreCaseAndActiveTrue("ROLE_ADMIN") <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể hạ quyền Quản trị viên hoạt động cuối cùng");
            }
        }
        user.setRole(normalized);
        return userRepository.save(user);
    }

    public User updateUserStatus(Long id, boolean active) {
        return updateUserStatus(id, active, null);
    }

    public User updateUserStatus(Long id, boolean active, String actorUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng ID: " + id));
        if (!active) {
            if (actorUsername != null && actorUsername.equalsIgnoreCase(user.getUsername())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quản trị viên không thể tự khóa tài khoản của chính mình");
            }
            if ("ROLE_ADMIN".equalsIgnoreCase(user.getRole()) && user.isActive()
                    && userRepository.countByRoleIgnoreCaseAndActiveTrue("ROLE_ADMIN") <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể khóa Quản trị viên hoạt động cuối cùng");
            }
        }
        user.setActive(active);
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        deleteUser(id, null);
    }

    public void deleteUser(Long id, String actorUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng ID: " + id));
        if (actorUsername != null && actorUsername.equalsIgnoreCase(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quản trị viên không thể tự xóa tài khoản của chính mình");
        }
        if ("ROLE_ADMIN".equalsIgnoreCase(user.getRole()) && user.isActive()
                && userRepository.countByRoleIgnoreCaseAndActiveTrue("ROLE_ADMIN") <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể xóa Quản trị viên hoạt động cuối cùng");
        }
        userRepository.delete(user);
    }

    private String normalizeRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return null;
        }
        String upper = rawRole.trim().toUpperCase(Locale.ROOT);
        return upper.startsWith("ROLE_") ? upper : "ROLE_" + upper;
    }
}
