package com.sports.management.auth.controller;

import com.sports.management.auth.dto.LoginRequest;
import com.sports.management.auth.dto.LoginResponse;
import com.sports.management.auth.dto.RegisterRequest;
import com.sports.management.auth.model.User;
import com.sports.management.auth.repository.UserRepository;
import com.sports.management.auth.service.AuthService;
import com.sports.management.auth.util.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final String instanceId;

    public AuthController(
            AuthService authService,
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            @Value("${eureka.instance.instance-id:${spring.application.name}:${server.port}}") String instanceId) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.instanceId = instanceId;
    }

    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> sayHello() {
        return ResponseEntity.ok(Map.of(
                "service", "Auth Service",
                "message", "Welcome to Auth & Identity Service API (TH5 - TH7)",
                "instanceId", instanceId
        ));
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("serviceName", "AUTH-SERVICE");
        info.put("version", "v1");
        info.put("description", "Quản lý tài khoản người dùng, xác thực BCrypt và cấp phát JWT Token (RBAC)");
        info.put("totalUsers", userRepository.count());
        info.put("instanceId", instanceId);
        return ResponseEntity.ok(info);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        User created = authService.register(request);
        Map<String, Object> response = new HashMap<>();
        response.put("id", created.getId());
        response.put("username", created.getUsername());
        response.put("fullName", created.getFullName());
        response.put("email", created.getEmail());
        response.put("role", created.getRole());
        response.put("message", "User registered successfully");
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Instance-Id", instanceId)
                .body(response);
    }

    private void assertAdmin(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        String role = jwtTokenProvider.getRoleFromToken(token);
        if (!"ROLE_ADMIN".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Only Administrator has permission to access user management");
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        assertAdmin(authHeader);
        List<Map<String, Object>> list = userRepository.findAll().stream().map(this::toUserMap).toList();
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(list);
    }

    @GetMapping("/users/paged")
    public ResponseEntity<Map<String, Object>> getUsersPaged(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        assertAdmin(authHeader);
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim().toLowerCase() : null;
        String safeRole = (role != null && !role.isBlank()) ? role.trim().toUpperCase() : null;

        List<Map<String, Object>> filtered = userRepository.findAll().stream()
                .filter(u -> kw == null
                        || (u.getUsername() != null && u.getUsername().toLowerCase().contains(kw))
                        || (u.getFullName() != null && u.getFullName().toLowerCase().contains(kw))
                        || (u.getEmail() != null && u.getEmail().toLowerCase().contains(kw)))
                .filter(u -> safeRole == null || (u.getRole() != null && u.getRole().equalsIgnoreCase(safeRole)))
                .map(this::toUserMap)
                .toList();

        int totalElements = filtered.size();
        int safeSize = Math.max(1, size);
        int safePage = Math.max(0, page);
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);
        int fromIndex = Math.min(safePage * safeSize, totalElements);
        int toIndex = Math.min(fromIndex + safeSize, totalElements);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", filtered.subList(fromIndex, toIndex));
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("totalElements", totalElements);
        result.put("totalPages", totalPages);

        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(result);
    }

    @GetMapping("/referees")
    public ResponseEntity<List<Map<String, Object>>> getActiveReferees(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        String role = jwtTokenProvider.getRoleFromToken(token);
        if (!"ROLE_ADMIN".equalsIgnoreCase(role) && !"ROLE_ORGANIZER".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Only Organizer or Admin can list referees");
        }
        List<Map<String, Object>> referees = userRepository.findAll().stream()
                .filter(u -> u.isActive() && "ROLE_REFEREE".equalsIgnoreCase(u.getRole()))
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("username", u.getUsername());
                    m.put("fullName", u.getFullName());
                    return m;
                })
                .toList();
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(referees);
    }

    private Map<String, Object> toUserMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("fullName", u.getFullName());
        m.put("email", u.getEmail());
        m.put("role", u.getRole());
        m.put("active", u.isActive());
        return m;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        String username = jwtTokenProvider.getUsernameFromToken(token);
        User user = authService.getUserByUsername(username);

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("fullName", user.getFullName());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        response.put("active", user.isActive());
        response.put("permissions", com.sports.management.auth.model.RolePermissions.getPermissionsForRole(user.getRole()));

        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(response);
    }

    @PutMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        if ((username == null || username.isBlank()) && authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtTokenProvider.validateToken(token)) {
                username = jwtTokenProvider.getUsernameFromToken(token);
            }
        }
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Yêu cầu đăng nhập hoặc cung cấp username");
        }
        authService.changePassword(username, payload.get("currentPassword"), payload.get("newPassword"));
        return ResponseEntity.ok(Map.of(
                "username", username,
                "message", "Đổi mật khẩu thành công"
        ));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        assertAdmin(authHeader);
        User updated = authService.updateUserRole(id, payload.get("role"));
        return ResponseEntity.ok(Map.of(
                "id", updated.getId(),
                "username", updated.getUsername(),
                "role", updated.getRole(),
                "active", updated.isActive(),
                "message", "Cập nhật vai trò thành công"
        ));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<Map<String, Object>> updateUserStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        assertAdmin(authHeader);
        boolean active = payload.get("active") == null || Boolean.parseBoolean(String.valueOf(payload.get("active")));
        User updated = authService.updateUserStatus(id, active);
        return ResponseEntity.ok(Map.of(
                "id", updated.getId(),
                "username", updated.getUsername(),
                "role", updated.getRole(),
                "active", updated.isActive(),
                "message", "Cập nhật trạng thái tài khoản thành công"
        ));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        assertAdmin(authHeader);
        authService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
