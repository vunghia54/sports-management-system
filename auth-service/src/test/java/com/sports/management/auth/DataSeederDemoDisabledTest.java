package com.sports.management.auth;

import com.sports.management.auth.config.DataSeeder;
import com.sports.management.auth.repository.UserRepository;
import com.sports.management.auth.util.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth_demo_off_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "jwt.secret=SportsManagementSystemSuperSecretKeyWithAtLeast256BitsLongForHMACSHA256!"
})
class DataSeederDemoDisabledTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserRepository userRepository;

    @Test
    void defaultProfile_doesNotLoadDataSeederOrSeedDemoUsers() {
        assertTrue(applicationContext.getBeansOfType(DataSeeder.class).isEmpty(),
                "DataSeeder must not be active when app.demo.seed-users is false");
        assertEquals(0L, userRepository.count(),
                "No demo users may be automatically seeded when demo mode is disabled");
        assertEquals(3600L, applicationContext.getBean(JwtTokenProvider.class).getJwtExpirationSeconds(),
                "jwt.expiration must be 3600 seconds (1 hour)");
    }
}
