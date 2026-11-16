package com.sports.management.result.client;

import com.sports.management.exception.PermanentSyncException;
import com.sports.management.exception.ResourceNotFoundException;
import com.sports.management.exception.TransientSyncException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MatchAndRankingClient {

    private final RestClient matchRestClient;
    private final RestClient rankingRestClient;
    private final RestClient tournamentRestClient;
    private final String matchServiceUrl;
    private volatile String lastHandledMatchInstance = "N/A";

    @Autowired
    public MatchAndRankingClient(
            @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
            @Qualifier("defaultRestClientBuilder") RestClient.Builder defaultRestClientBuilder,
            @Value("${services.match.url:http://match-service}") String matchServiceUrl,
            @Value("${services.ranking.url:http://ranking-service}") String rankingServiceUrl,
            @Value("${services.tournament.url:http://tournament-service}") String tournamentServiceUrl) {

        this.matchServiceUrl = matchServiceUrl;
        this.matchRestClient = buildClient(loadBalancedRestClientBuilder, defaultRestClientBuilder, matchServiceUrl);
        this.rankingRestClient = buildClient(loadBalancedRestClientBuilder, defaultRestClientBuilder, rankingServiceUrl);
        this.tournamentRestClient = buildClient(loadBalancedRestClientBuilder, defaultRestClientBuilder, tournamentServiceUrl);
    }

    public MatchAndRankingClient(
            RestClient.Builder loadBalancedRestClientBuilder,
            String matchServiceUrl,
            String rankingServiceUrl) {
        this(loadBalancedRestClientBuilder, loadBalancedRestClientBuilder, matchServiceUrl, rankingServiceUrl, "http://tournament-service");
    }

    private static RestClient buildClient(
            RestClient.Builder lbBuilder,
            RestClient.Builder plainBuilder,
            String baseUrl) {
        boolean isDirectLocalhost = baseUrl != null
                && (baseUrl.startsWith("http://localhost") || baseUrl.startsWith("http://127.0.0.1"));
        RestClient.Builder chosen = (isDirectLocalhost && plainBuilder != null) ? plainBuilder : lbBuilder;
        return chosen.clone().baseUrl(baseUrl).build();
    }

    public Map<String, Object> getMatchById(Long matchId) {
        if (matchId == null) {
            throw new IllegalArgumentException("Match ID is required");
        }
        try {
            var requestSpec = matchRestClient.get()
                    .uri("/api/v1/matches/{id}", matchId);
            String authHeader = getCurrentAuthHeader();
            if (authHeader != null && !authHeader.isBlank()) {
                requestSpec.header("Authorization", authHeader);
            }

            ResponseEntity<Map<String, Object>> response = requestSpec.retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new ResourceNotFoundException("Match not found with id: " + matchId);
                    })
                    .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

            String instanceId = response.getHeaders().getFirst("X-Instance-Id");
            if (instanceId != null && !instanceId.isBlank()) {
                this.lastHandledMatchInstance = instanceId;
            }
            return response.getBody();
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Match Service (MATCH-SERVICE) is unavailable via Service Discovery",
                    ex
            );
        }
    }

    public Map<String, Object> getTournamentById(Long tournamentId) {
        if (tournamentId == null) {
            return null;
        }
        try {
            var requestSpec = tournamentRestClient.get()
                    .uri("/api/v1/tournaments/{id}", tournamentId);
            String authHeader = getCurrentAuthHeader();
            if (authHeader != null && !authHeader.isBlank()) {
                requestSpec.header("Authorization", authHeader);
            }

            ResponseEntity<Map<String, Object>> response = requestSpec.retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new TournamentNotFoundException();
                    })
                    .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});
            return response.getBody();
        } catch (TournamentNotFoundException ex) {
            return null;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Tournament Service (TOURNAMENT-SERVICE) is unavailable via Service Discovery",
                    ex
            );
        }
    }

    public Map<String, Object> getCompetitionFormat(Long competitionId) {
        if (competitionId == null) {
            return null;
        }
        try {
            var requestSpec = tournamentRestClient.get()
                    .uri("/api/v1/competitions/{id}/format", competitionId);
            String authHeader = getCurrentAuthHeader();
            if (authHeader != null && !authHeader.isBlank()) {
                requestSpec.header("Authorization", authHeader);
            }
            return requestSpec.retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new TournamentNotFoundException();
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (TournamentNotFoundException ex) {
            return null;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Tournament Service is unavailable when verifying competition format #" + competitionId,
                    ex
            );
        }
    }

    public void markMatchFinished(Long matchId) {
        markMatchFinished(matchId, null);
    }

    public void markMatchFinished(Long matchId, String bearerToken) {
        String auth = (bearerToken != null && !bearerToken.isBlank()) ? bearerToken : getCurrentAuthHeader();
        try {
            var requestSpec = matchRestClient.patch()
                    .uri("/api/v1/matches/{id}", matchId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("status", "FINISHED"));
            if (auth != null && !auth.isBlank()) {
                requestSpec.header("Authorization", auth);
            }
            requestSpec.retrieve().toBodilessEntity();
        } catch (HttpClientErrorException ex) {
            int code = ex.getStatusCode().value();
            if (code == 408 || code == 429) {
                throw new TransientSyncException(
                        "Match Service tạm thời quá tải/timeout (HTTP " + code + ") khi cập nhật trận #" + matchId, ex);
            }
            throw new PermanentSyncException(
                    code,
                    "Match Service từ chối cập nhật trận #" + matchId + " sang FINISHED (HTTP " + code + ")",
                    ex
            );
        } catch (Exception ex) {
            throw new TransientSyncException(
                    "Không thể cập nhật trạng thái trận #" + matchId + " qua Match Service: " + ex.getMessage(),
                    ex
            );
        }
    }

    public void recalculateRanking(Long competitionId) {
        recalculateRanking(competitionId, null);
    }

    public void recalculateRanking(Long competitionId, String bearerToken) {
        if (competitionId == null) {
            return;
        }
        String auth = (bearerToken != null && !bearerToken.isBlank()) ? bearerToken : getCurrentAuthHeader();
        try {
            var requestSpec = rankingRestClient.post()
                    .uri("/api/v1/rankings/competition/{competitionId}/recalculate", competitionId);
            if (auth != null && !auth.isBlank()) {
                requestSpec.header("Authorization", auth);
            }
            requestSpec.retrieve().toBodilessEntity();
        } catch (HttpClientErrorException ex) {
            int code = ex.getStatusCode().value();
            if (code == 408 || code == 429) {
                throw new TransientSyncException(
                        "Ranking Service tạm thời quá tải/timeout (HTTP " + code + ") cho nội dung #" + competitionId, ex);
            }
            throw new PermanentSyncException(
                    code,
                    "Ranking Service từ chối tính lại BXH cho nội dung #" + competitionId + " (HTTP " + code + ")",
                    ex
            );
        } catch (Exception ex) {
            throw new TransientSyncException(
                    "Không thể tính lại BXH cho nội dung #" + competitionId + " qua Ranking Service: " + ex.getMessage(),
                    ex
            );
        }
    }

    public Map<String, Object> discoverMatchInstance() {
        try {
            Map<String, Object> providerInfo = matchRestClient.get()
                    .uri("/api/v1/instance-info")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("consumerService", "RESULT-SERVICE");
            result.put("discoveryTarget", matchServiceUrl);
            result.put("providerInstance", providerInfo);
            return result;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Match Service (MATCH-SERVICE) has no available instances in Eureka Registry",
                    ex
            );
        }
    }

    public String getLastHandledMatchInstance() {
        return lastHandledMatchInstance;
    }

    private String getCurrentAuthHeader() {
        var requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            HttpServletRequest request = servletRequestAttributes.getRequest();
            return request.getHeader("Authorization");
        }
        return null;
    }

    private static class TournamentNotFoundException extends RuntimeException {
    }
}
