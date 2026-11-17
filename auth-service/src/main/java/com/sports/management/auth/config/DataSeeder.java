package com.sports.management.auth.config;

import com.sports.management.auth.model.User;
import com.sports.management.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.demo.seed-users", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUserIfNotExists("admin", "admin123", "Quản Trị Viên Hệ Thống", "admin@sports.vn", "ROLE_ADMIN");
        seedUserIfNotExists("organizer1", "organizer123", "Ban Tổ Chức Giải Đấu 1", "organizer1@sports.vn", "ROLE_ORGANIZER");
        seedUserIfNotExists("organizer2", "organizer123", "Ban Tổ Chức Giải Đấu 2", "organizer2@sports.vn", "ROLE_ORGANIZER");
        seedUserIfNotExists("referee1", "referee123", "Trọng Tài Quốc Gia FIFA", "referee1@sports.vn", "ROLE_REFEREE");
        seedUserIfNotExists("manager1", "manager123", "HLV Trưởng CLB Hà Nội", "manager1@sports.vn", "ROLE_TEAM_MANAGER");
        seedUserIfNotExists("user1", "user123", "Khán Giả Thể Thao", "user1@sports.vn", "ROLE_VIEWER");
    }

    private void seedUserIfNotExists(String username, String rawPassword, String fullName, String email, String role) {
        String normalized = com.sports.management.auth.service.AuthService.normalizeUsername(username);
        if (normalized != null && !normalized.isEmpty() && userRepository.findAllByNormalizedUsername(normalized).isEmpty()) {
            User user = new User();
            user.setUsername(normalized);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setFullName(fullName);
            user.setEmail(email);
            user.setRole(role);
            user.setActive(true);
            userRepository.save(user);
        }
    }
}
