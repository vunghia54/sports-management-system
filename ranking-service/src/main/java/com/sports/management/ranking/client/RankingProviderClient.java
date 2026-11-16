package com.sports.management.ranking.client;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RankingProviderClient {

    private final RestClient tournamentRestClient;
    private final RestClient matchRestClient;
    private final RestClient resultRestClient;

    public RankingProviderClient(
            @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
            @Value("${services.tournament.url:http://tournament-service}") String tournamentServiceUrl,
            @Value("${services.match.url:http://match-service}") String matchServiceUrl,
            @Value("${services.result.url:http://result-service}") String resultServiceUrl) {

        String normTour = normalizeUrl(tournamentServiceUrl, "http://tournament-service");
        String normMatch = normalizeUrl(matchServiceUrl, "http://match-service");
        String normResult = normalizeUrl(resultServiceUrl, "http://result-service");

        this.tournamentRestClient = selectBuilder(loadBalancedRestClientBuilder, normTour)
                .baseUrl(normTour)
                .build();
        this.matchRestClient = selectBuilder(loadBalancedRestClientBuilder, normMatch)
                .baseUrl(normMatch)
                .build();
        this.resultRestClient = selectBuilder(loadBalancedRestClientBuilder, normResult)
                .baseUrl(normResult)
                .build();
    }

    private static String normalizeUrl(String raw, String fallback) {
        String candidate = (raw == null || raw.isBlank()) ? fallback : raw.trim();
        return candidate.endsWith("/") ? candidate.substring(0, candidate.length() - 1) : candidate;
    }

    private static RestClient.Builder selectBuilder(RestClient.Builder lbBuilder, String baseUrl) {
        if (baseUrl.startsWith("http://localhost") || baseUrl.startsWith("http://127.0.0.1")) {
            return RestClient.builder();
        }
        return lbBuilder != null ? lbBuilder.clone() : RestClient.builder();
    }

    public Map<String, Object> getTournamentById(Long tournamentId) {
        if (tournamentId == null) {
            return null;
        }
        try {
            var spec = tournamentRestClient.get()
                    .uri("/api/v1/tournaments/{id}", tournamentId);
            String auth = getCurrentAuthHeader();
            if (auth != null) spec.header("Authorization", auth);
            return spec.retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (HttpClientErrorException.NotFound nf) {
            return null;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Tournament Service is unavailable when loading Tournament ID: " + tournamentId,
                    ex
            );
        }
    }

    public Map<String, Object> getCompetitionById(Long competitionId) {
        try {
            var spec = tournamentRestClient.get()
                    .uri("/api/v1/competitions/{id}", competitionId);
            String auth = getCurrentAuthHeader();
            if (auth != null) spec.header("Authorization", auth);
            return spec.retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (HttpClientErrorException.NotFound nf) {
            return null;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Tournament Service is unavailable when loading Competition ID: " + competitionId,
                    ex
            );
        }
    }

    public Map<String, Object> getCompetitionFormat(Long competitionId) {
        try {
            var spec = tournamentRestClient.get()
                    .uri("/api/v1/competitions/{id}/format", competitionId);
            String auth = getCurrentAuthHeader();
            if (auth != null) spec.header("Authorization", auth);
            return spec.retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (HttpClientErrorException.NotFound nf) {
            return null;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Tournament Service is unavailable when loading format for Competition ID: " + competitionId,
                    ex
            );
        }
    }

    public List<Map<String, Object>> getMatchesByCompetitionId(Long competitionId) {
        try {
            var spec = matchRestClient.get()
                    .uri("/api/v1/matches/competition/{competitionId}", competitionId);
            String auth = getCurrentAuthHeader();
            if (auth != null) spec.header("Authorization", auth);
            List<Map<String, Object>> matches = spec.retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            return matches != null ? matches : Collections.emptyList();
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Match Service is unavailable when recalculating ranking for competition " + competitionId,
                    ex
            );
        }
    }

    public List<Map<String, Object>> getResultsByMatchIds(Collection<Long> matchIds) {
        if (matchIds == null || matchIds.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            var spec = resultRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/results/by-matches")
                            .queryParam("matchIds", matchIds.toArray())
                            .build());
            String auth = getCurrentAuthHeader();
            if (auth != null) spec.header("Authorization", auth);
            List<Map<String, Object>> results = spec.retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            return results != null ? results : Collections.emptyList();
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Result Service is unavailable when recalculating ranking",
                    ex
            );
        }
    }

    public List<Map<String, Object>> getAllResults() {
        try {
            var spec = resultRestClient.get()
                    .uri("/api/v1/results");
            String auth = getCurrentAuthHeader();
            if (auth != null) spec.header("Authorization", auth);
            List<Map<String, Object>> results = spec.retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            return results != null ? results : Collections.emptyList();
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Result Service is unavailable when recalculating ranking",
                    ex
            );
        }
    }

    public Map<String, Object> discoverProviders() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("consumerService", "RANKING-SERVICE");
        try {
            summary.put("tournamentProvider", tournamentRestClient.get()
                    .uri("/api/v1/instance-info")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {}));
        } catch (Exception e) {
            summary.put("tournamentProvider", Map.of("status", "UNAVAILABLE"));
        }
        try {
            summary.put("matchProvider", matchRestClient.get()
                    .uri("/api/v1/instance-info")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {}));
        } catch (Exception e) {
            summary.put("matchProvider", Map.of("status", "UNAVAILABLE"));
        }
        try {
            summary.put("resultProvider", resultRestClient.get()
                    .uri("/api/v1/instance-info")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {}));
        } catch (Exception e) {
            summary.put("resultProvider", Map.of("status", "UNAVAILABLE"));
        }
        return summary;
    }

    private String getCurrentAuthHeader() {
        var requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            HttpServletRequest request = servletRequestAttributes.getRequest();
            return request.getHeader("Authorization");
        }
        return null;
    }
}

