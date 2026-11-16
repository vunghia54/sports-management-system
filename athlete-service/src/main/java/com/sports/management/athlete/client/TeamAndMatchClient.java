package com.sports.management.athlete.client;

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
public class TeamAndMatchClient {

    private final RestClient teamRestClient;
    private final RestClient matchRestClient;

    public TeamAndMatchClient(
            @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
            @Value("${services.team.url:http://team-service}") String teamServiceUrl,
            @Value("${services.match.url:http://match-service}") String matchServiceUrl) {
        String normTeam = normalizeUrl(teamServiceUrl, "http://team-service");
        String normMatch = normalizeUrl(matchServiceUrl, "http://match-service");
        this.teamRestClient = selectBuilder(loadBalancedRestClientBuilder, normTeam).baseUrl(normTeam).build();
        this.matchRestClient = selectBuilder(loadBalancedRestClientBuilder, normMatch).baseUrl(normMatch).build();
    }

    private static String normalizeUrl(String raw, String fallback) {
        String candidate = (raw == null || raw.isBlank()) ? fallback : raw.trim();
        return candidate.endsWith("/") ? candidate.substring(0, candidate.length() - 1) : candidate;
    }

    private static RestClient.Builder selectBuilder(RestClient.Builder lbBuilder, String baseUrl) {
        if (baseUrl.startsWith("http://localhost") || baseUrl.startsWith("http://127.0.0.1")) {
            return RestClient.builder();
        }
        return lbBuilder != null ? lbBuilder : RestClient.builder();
    }

    public boolean isAthleteUsedInTeams(Long athleteId) {
        if (athleteId == null) {
            return false;
        }
        try {
            var spec = teamRestClient.get().uri("/api/v1/teams/athletes/{id}/usage", athleteId);
            String auth = getCurrentAuthHeader();
            if (auth != null && !auth.isBlank()) {
                spec.header("Authorization", auth);
            }
            Map<String, Object> resp = spec.retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            return resp != null && Boolean.TRUE.equals(resp.get("inTeam"));
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Team Service is unavailable when verifying memberships before deleting athlete #" + athleteId,
                    ex
            );
        }
    }

    public boolean isAthleteUsedInMatches(Long athleteId) {
        if (athleteId == null) {
            return false;
        }
        try {
            var spec = matchRestClient.get()
                    .uri("/api/v1/participants/usage?participantType=ATHLETE&referenceId={id}", athleteId);
            String auth = getCurrentAuthHeader();
            if (auth != null && !auth.isBlank()) {
                spec.header("Authorization", auth);
            }
            Map<String, Object> resp = spec.retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            return resp != null && Boolean.TRUE.equals(resp.get("inUse"));
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Match Service is unavailable when verifying registrations/participants before deleting athlete #" + athleteId,
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
