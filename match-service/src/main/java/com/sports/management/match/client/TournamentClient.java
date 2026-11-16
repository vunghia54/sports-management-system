package com.sports.management.match.client;

import com.sports.management.exception.ResourceNotFoundException;
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TournamentClient {

    private final RestClient tournamentRestClient;
    private final RestClient teamRestClient;
    private final RestClient athleteRestClient;
    private final String tournamentServiceUrl;
    private volatile String lastHandledInstance = "N/A";

    public TournamentClient(
            @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
            @Value("${services.tournament.url:http://tournament-service}") String tournamentServiceUrl,
            @Value("${services.team.url:http://team-service}") String teamServiceUrl,
            @Value("${services.athlete.url:http://athlete-service}") String athleteServiceUrl) {

        this.tournamentServiceUrl = tournamentServiceUrl;
        this.tournamentRestClient = loadBalancedRestClientBuilder.clone()
                .baseUrl(tournamentServiceUrl)
                .build();
        this.teamRestClient = loadBalancedRestClientBuilder.clone()
                .baseUrl(teamServiceUrl)
                .build();
        this.athleteRestClient = loadBalancedRestClientBuilder.clone()
                .baseUrl(athleteServiceUrl)
                .build();
    }

    public boolean tournamentExists(Long tournamentId) {
        if (tournamentId == null) {
            return false;
        }

        Exception lastException = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                var requestSpec = tournamentRestClient.get()
                        .uri("/api/v1/tournaments/{id}", tournamentId);

                String authHeader = getCurrentAuthHeader();
                if (authHeader != null && !authHeader.isBlank()) {
                    requestSpec.header("Authorization", authHeader);
                }

                ResponseEntity<Void> response = requestSpec.retrieve()
                        .onStatus(
                                status -> status.value() == 404,
                                (req, res) -> {
                                    throw new TournamentNotFoundException();
                                }
                        )
                        .toBodilessEntity();

                String instanceId = response.getHeaders().getFirst("X-Instance-Id");
                if (instanceId != null && !instanceId.isBlank()) {
                    this.lastHandledInstance = instanceId;
                    System.out.println("[SERVICE DISCOVERY] MatchService -> " + tournamentServiceUrl
                            + "/api/v1/tournaments/" + tournamentId + " handled by instance: " + instanceId);
                }

                return true;

            } catch (TournamentNotFoundException ex) {
                return false;

            } catch (Exception ex) {
                lastException = ex;
            }
        }

        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Tournament Service (TOURNAMENT-SERVICE) is temporarily unavailable via Service Discovery",
                lastException
        );
    }

    public Map<String, Object> getTournamentById(Long tournamentId) {
        if (tournamentId == null) {
            return null;
        }
        Exception lastException = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
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
                String instanceId = response.getHeaders().getFirst("X-Instance-Id");
                if (instanceId != null && !instanceId.isBlank()) {
                    this.lastHandledInstance = instanceId;
                }
                return response.getBody();
            } catch (TournamentNotFoundException ex) {
                return null;
            } catch (Exception ex) {
                lastException = ex;
            }
        }
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Tournament Service (TOURNAMENT-SERVICE) is unavailable when verifying Tournament ID: " + tournamentId,
                lastException
        );
    }

    public java.util.List<Map<String, Object>> getCompetitionsByTournament(Long tournamentId) {
        if (tournamentId == null) {
            return java.util.Collections.emptyList();
        }
        try {
            var requestSpec = tournamentRestClient.get()
                    .uri("/api/v1/competitions?tournamentId={id}", tournamentId);
            String authHeader = getCurrentAuthHeader();
            if (authHeader != null && !authHeader.isBlank()) {
                requestSpec.header("Authorization", authHeader);
            }
            java.util.List<Map<String, Object>> list = requestSpec.retrieve()
                    .body(new ParameterizedTypeReference<java.util.List<Map<String, Object>>>() {});
            return list != null ? list : java.util.Collections.emptyList();
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Tournament Service is unavailable when loading competitions for Tournament ID: " + tournamentId,
                    ex
            );
        }
    }

    public Map<String, Object> getCompetitionById(Long competitionId) {
        try {
            var requestSpec = tournamentRestClient.get()
                    .uri("/api/v1/competitions/{id}", competitionId);
            String authHeader = getCurrentAuthHeader();
            if (authHeader != null && !authHeader.isBlank()) {
                requestSpec.header("Authorization", authHeader);
            }
            return requestSpec.retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new ResourceNotFoundException("Competition not found with id: " + competitionId);
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Tournament Service is unavailable when verifying Competition ID: " + competitionId,
                    ex
            );
        }
    }

    public Map<String, Object> getTeamById(Long teamId) {
        try {
            var requestSpec = teamRestClient.get()
                    .uri("/api/v1/teams/{id}", teamId);
            String authHeader = getCurrentAuthHeader();
            if (authHeader != null && !authHeader.isBlank()) {
                requestSpec.header("Authorization", authHeader);
            }
            return requestSpec.retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new ResourceNotFoundException("Team not found with id: " + teamId);
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Team Service (TEAM-SERVICE) is unavailable via Service Discovery",
                    ex
            );
        }
    }

    public Map<String, Object> getAthleteById(Long athleteId) {
        try {
            var requestSpec = athleteRestClient.get()
                    .uri("/api/v1/athletes/{id}", athleteId);
            String authHeader = getCurrentAuthHeader();
            if (authHeader != null && !authHeader.isBlank()) {
                requestSpec.header("Authorization", authHeader);
            }
            return requestSpec.retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new ResourceNotFoundException("Athlete not found with id: " + athleteId);
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Athlete Service (ATHLETE-SERVICE) is unavailable via Service Discovery",
                    ex
            );
        }
    }

    public Map<String, Object> getSportById(Long sportId) {
        if (sportId == null) {
            return null;
        }
        try {
            var requestSpec = tournamentRestClient.get()
                    .uri("/api/v1/sports/{id}", sportId);
            String authHeader = getCurrentAuthHeader();
            if (authHeader != null && !authHeader.isBlank()) {
                requestSpec.header("Authorization", authHeader);
            }
            return requestSpec.retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new ResourceNotFoundException("Sport not found with id: " + sportId);
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Tournament Service is unavailable when verifying Sport ID: " + sportId,
                    ex
            );
        }
    }

    public Map<String, Object> getCompetitionRule(Long competitionId) {
        if (competitionId == null) {
            return null;
        }
        try {
            var requestSpec = tournamentRestClient.get()
                    .uri("/api/v1/competitions/{id}/rule", competitionId);
            String authHeader = getCurrentAuthHeader();
            if (authHeader != null && !authHeader.isBlank()) {
                requestSpec.header("Authorization", authHeader);
            }
            return requestSpec.retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new ResourceNotFoundException("Rule not found for competition: " + competitionId);
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (ResourceNotFoundException ex) {
            return null;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Tournament Service is unavailable when loading CompetitionRule for Competition ID: " + competitionId,
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
                        throw new ResourceNotFoundException("Format not found for competition: " + competitionId);
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (ResourceNotFoundException ex) {
            return null;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Tournament Service is unavailable when loading CompetitionFormat for Competition ID: " + competitionId,
                    ex
            );
        }
    }

    public List<Map<String, Object>> getTeamMembers(Long teamId) {
        if (teamId == null) {
            return Collections.emptyList();
        }
        try {
            var requestSpec = teamRestClient.get()
                    .uri("/api/v1/teams/{id}/members", teamId);
            String authHeader = getCurrentAuthHeader();
            if (authHeader != null && !authHeader.isBlank()) {
                requestSpec.header("Authorization", authHeader);
            }
            List<Map<String, Object>> list = requestSpec.retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new ResourceNotFoundException("Team not found with id: " + teamId);
                    })
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            return list != null ? list : Collections.emptyList();
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Team Service is unavailable when loading members for Team ID: " + teamId,
                    ex
            );
        }
    }

    public Map<String, Object> discoverTournamentInstance() {
        Exception lastException = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                Map<String, Object> providerInfo = tournamentRestClient.get()
                        .uri("/api/v1/instance-info")
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("consumerService", "MATCH-SERVICE");
                result.put("discoveryTarget", tournamentServiceUrl);
                result.put("providerInstance", providerInfo);
                if (providerInfo != null && providerInfo.get("instanceId") != null) {
                    this.lastHandledInstance = String.valueOf(providerInfo.get("instanceId"));
                }
                return result;
            } catch (Exception ex) {
                lastException = ex;
            }
        }
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Tournament Service (TOURNAMENT-SERVICE) has no available instances in Eureka Registry",
                lastException
        );
    }

    public String getLastHandledInstance() {
        return lastHandledInstance;
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
