package com.sports.management.match.config;

import com.sports.management.match.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public APIs
                        .requestMatchers(HttpMethod.GET, "/api/v1/matches/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/matches/check-conflict").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/registrations/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/participants/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/instance-info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Protected APIs
                        .requestMatchers(HttpMethod.POST, "/api/v1/matches/**").hasAnyAuthority("ROLE_ORGANIZER", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/matches/**").hasAnyAuthority("ROLE_REFEREE", "ROLE_ORGANIZER", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/matches/**").hasAnyAuthority("ROLE_REFEREE", "ROLE_ORGANIZER", "ROLE_ADMIN", "ROLE_INTERNAL_RESULT_SYNC")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/matches/**").hasAnyAuthority("ROLE_ORGANIZER", "ROLE_ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/v1/registrations/**").hasAnyAuthority("ROLE_TEAM_MANAGER", "ROLE_VIEWER", "ROLE_USER", "ROLE_ORGANIZER", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/registrations/*/withdraw").hasAnyAuthority("ROLE_TEAM_MANAGER", "ROLE_VIEWER", "ROLE_USER", "ROLE_ORGANIZER", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/registrations/**").hasAnyAuthority("ROLE_ORGANIZER", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/registrations/**").hasAnyAuthority("ROLE_ORGANIZER", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/registrations/**").hasAnyAuthority("ROLE_ORGANIZER", "ROLE_ADMIN")

                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"status\":401,\"message\":\"Full authentication is required to access this resource\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"status\":403,\"message\":\"Access Denied: You do not have permission to access this resource\"}");
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
