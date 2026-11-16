package com.sports.management.auth;

import com.sports.management.auth.config.DataSeeder;
import com.sports.management.auth.model.User;
import com.sports.management.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth_demo_on_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false"
})
@ActiveProfiles("demo")
class DataSeederDemoEnabledTest {

    @Autowired
    private DataSeeder dataSeeder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void demoProfile_seedsDemoUsersWithoutOverwritingExistingAccounts() {
        assertNotNull(dataSeeder);
        assertTrue(userRepository.existsByUsername("admin"));
        assertTrue(userRepository.existsByUsername("organizer1"));
        assertTrue(userRepository.existsByUsername("organizer2"));
        assertTrue(userRepository.existsByUsername("referee1"));
        assertTrue(userRepository.existsByUsername("manager1"));
        assertTrue(userRepository.existsByUsername("user1"));
        assertEquals(6L, userRepository.count());

        // Simulate an admin whose password was changed by the operator
        User existingAdmin = userRepository.findByUsername("admin").orElseThrow();
        existingAdmin.setPassword(passwordEncoder.encode("custom-secret-999"));
        userRepository.save(existingAdmin);

        dataSeeder.run();

        User afterReseed = userRepository.findByUsername("admin").orElseThrow();
        assertTrue(passwordEncoder.matches("custom-secret-999", afterReseed.getPassword()),
                "Re-running DataSeeder must preserve existing account passwords");
    }
}
