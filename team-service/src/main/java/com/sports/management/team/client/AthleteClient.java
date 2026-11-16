package com.sports.management.team.client;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AthleteClient {

    private final RestClient restClient;
    private final String athleteServiceUrl;
    private volatile String lastHandledInstance = "N/A";

    public AthleteClient(
            @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
            @Value("${services.athlete.url:http://athlete-service}") String athleteServiceUrl) {
        this.athleteServiceUrl = athleteServiceUrl;
        this.restClient = loadBalancedRestClientBuilder
                .baseUrl(athleteServiceUrl)
                .build();
    }

    public Map<String, Object> getAthleteById(Long athleteId) {
        if (athleteId == null) {
            return null;
        }
        Exception lastException = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                var spec = restClient.get().uri("/api/v1/athletes/{id}", athleteId);
                String authHeader = getCurrentAuthHeader();
                if (authHeader != null && !authHeader.isBlank()) {
                    spec.header("Authorization", authHeader);
                }

                ResponseEntity<Map<String, Object>> response = spec.retrieve()
                        .onStatus(status -> status.value() == 404, (req, res) -> {
                            throw new AthleteNotFoundException();
                        })
                        .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

                String instanceId = response.getHeaders().getFirst("X-Instance-Id");
                if (instanceId != null) {
                    this.lastHandledInstance = instanceId;
                }
                return response.getBody();
            } catch (AthleteNotFoundException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Athlete does not exist with id: " + athleteId);
            } catch (Exception ex) {
                lastException = ex;
            }
        }
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Athlete Service (ATHLETE-SERVICE) is temporarily unavailable via Service Discovery",
                lastException
        );
    }

    public Map<String, Object> discoverAthleteInstance() {
        try {
            Map<String, Object> providerInfo = restClient.get()
                    .uri("/api/v1/instance-info")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("consumerService", "TEAM-SERVICE");
            result.put("discoveryTarget", athleteServiceUrl);
            result.put("providerInstance", providerInfo);
            return result;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Athlete Service (ATHLETE-SERVICE) has no available instances in Eureka Registry",
                    ex
            );
        }
    }

    public String getLastHandledInstance() {
        return lastHandledInstance;
    }

    private String getCurrentAuthHeader() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            return request.getHeader("Authorization");
        }
        return null;
    }

    private static class AthleteNotFoundException extends RuntimeException {
    }
}

