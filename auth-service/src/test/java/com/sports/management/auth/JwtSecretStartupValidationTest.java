package com.sports.management.auth;

import com.sports.management.auth.util.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtSecretStartupValidationTest {

    @Test
    void jwtTokenProvider_failsFastWhenJwtSecretMissing() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(PropertySourcesPlaceholderConfigurer.class, JwtTokenProvider.class);
        assertThrows(BeanCreationException.class, ctx::refresh);
        ctx.close();
    }

    @Test
    void jwtTokenProvider_failsFastWhenJwtSecretBlankOrTooShort() {
        AnnotationConfigApplicationContext shortCtx = new AnnotationConfigApplicationContext();
        shortCtx.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("test", Map.of("jwt.secret", "too-short"))
        );
        shortCtx.register(PropertySourcesPlaceholderConfigurer.class, JwtTokenProvider.class);
        BeanCreationException ex = assertThrows(BeanCreationException.class, shortCtx::refresh);
        assertTrue(ex.getCause() instanceof IllegalStateException);
        shortCtx.close();
    }

    @Test
    void jwtTokenProvider_startsAndSignsWhenValidSecretProvided() {
        AnnotationConfigApplicationContext validCtx = new AnnotationConfigApplicationContext();
        validCtx.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("test", Map.of("jwt.secret", "ValidSecretKeyProvidedViaEnvWithAtLeast32Bytes!"))
        );
        validCtx.register(PropertySourcesPlaceholderConfigurer.class, JwtTokenProvider.class);
        validCtx.refresh();
        JwtTokenProvider provider = validCtx.getBean(JwtTokenProvider.class);
        String token = provider.generateToken("tester", 1L, "ROLE_VIEWER");
        assertTrue(provider.validateToken(token));
        validCtx.close();
    }
}
