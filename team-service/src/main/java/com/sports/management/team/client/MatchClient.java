package com.sports.management.team.client;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Component
public class MatchClient {

    private final RestClient restClient;

    public MatchClient(
            @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
            @Value("${services.match.url:http://match-service}") String matchServiceUrl) {
        String candidate = (matchServiceUrl == null || matchServiceUrl.isBlank())
                ? "http://match-service"
                : matchServiceUrl.trim();
        String normalized = candidate.endsWith("/") ? candidate.substring(0, candidate.length() - 1) : candidate;
        RestClient.Builder builder = (normalized.startsWith("http://localhost")
                || normalized.startsWith("http://127.0.0.1"))
                ? RestClient.builder()
                : loadBalancedRestClientBuilder;
        this.restClient = builder.baseUrl(normalized).build();
    }

    public boolean isTeamUsedInMatchesOrRegistrations(Long teamId) {
        if (teamId == null) {
            return false;
        }
        try {
            var spec = restClient.get()
                    .uri("/api/v1/participants/usage?participantType=TEAM&referenceId={id}", teamId);
            String authHeader = getCurrentAuthHeader();
            if (authHeader != null && !authHeader.isBlank()) {
                spec.header("Authorization", authHeader);
            }
            Map<String, Object> response = spec.retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            return response != null && Boolean.TRUE.equals(response.get("inUse"));
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Match Service is unavailable when verifying registrations/participants before deleting team #" + teamId,
                    ex
            );
        }
    }

    private String getCurrentAuthHeader() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            return request.getHeader("Authorization");
        }
        return null;
    }
}
