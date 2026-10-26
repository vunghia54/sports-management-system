package com.sports.management.auth.config;

import com.sports.management.auth.model.User;
import com.sports.management.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUserIfNotExists("admin", "admin123", "System Administrator", "admin@sports.com", "ROLE_ADMIN");
        seedUserIfNotExists("organizer1", "organizer123", "Tournament Organizer One", "organizer1@sports.com", "ROLE_ORGANIZER");
        seedUserIfNotExists("organizer2", "organizer123", "Tournament Organizer Two", "organizer2@sports.com", "ROLE_ORGANIZER");
        seedUserIfNotExists("referee1", "referee123", "Official Referee One", "referee1@sports.com", "ROLE_REFEREE");
        seedUserIfNotExists("user1", "user123", "Standard User One", "user1@sports.com", "ROLE_USER");
    }

    private void seedUserIfNotExists(String username, String rawPassword, String fullName, String email, String role) {
        if (!userRepository.existsByUsername(username)) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setFullName(fullName);
            user.setEmail(email);
            user.setRole(role);
            user.setActive(true);
            userRepository.save(user);
        }
    }
}
