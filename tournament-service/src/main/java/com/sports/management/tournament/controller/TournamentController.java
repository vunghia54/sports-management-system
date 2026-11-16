package com.sports.management.tournament.controller;

import com.sports.management.tournament.model.Competition;
import com.sports.management.tournament.model.Tournament;
import com.sports.management.tournament.service.CompetitionService;
import com.sports.management.tournament.service.TournamentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tournaments")
public class TournamentController {

    private final TournamentService tournamentService;
    private final CompetitionService competitionService;
    private final RestClient matchRestClient;
    private final RestClient rankingRestClient;
    private final String matchServiceUrl;
    private final String rankingServiceUrl;

    @Value("${spring.application.name:tournament-service}")
    private String applicationName;

    @Value("${server.port:8081}")
    private int serverPort;

    public TournamentController(
            TournamentService tournamentService,
            CompetitionService competitionService,
            @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder) {
        this(tournamentService, competitionService, loadBalancedRestClientBuilder,
                "http://match-service", "http://ranking-service");
    }

    @Autowired
    public TournamentController(
            TournamentService tournamentService,
            CompetitionService competitionService,
            @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
            @Value("${services.match.url:http://match-service}") String matchServiceUrl,
            @Value("${services.ranking.url:http://ranking-service}") String rankingServiceUrl) {
        this.tournamentService = tournamentService;
        this.competitionService = competitionService;
        this.matchServiceUrl = normalizeBaseUrl(matchServiceUrl, "http://match-service");
        this.rankingServiceUrl = normalizeBaseUrl(rankingServiceUrl, "http://ranking-service");
        this.matchRestClient = buildRestClient(loadBalancedRestClientBuilder, this.matchServiceUrl);
        this.rankingRestClient = buildRestClient(loadBalancedRestClientBuilder, this.rankingServiceUrl);
    }

    private static String normalizeBaseUrl(String value, String fallback) {
        String candidate = (value == null || value.isBlank()) ? fallback : value.trim();
        return candidate.endsWith("/") ? candidate.substring(0, candidate.length() - 1) : candidate;
    }

    private static RestClient buildRestClient(RestClient.Builder loadBalancedBuilder, String baseUrl) {
        if (baseUrl.startsWith("http://localhost") || baseUrl.startsWith("http://127.0.0.1")) {
            return RestClient.builder().build();
        }
        return loadBalancedBuilder != null ? loadBalancedBuilder.build() : RestClient.builder().build();
    }

    // TH1: Hello & Service Info & Statistics
    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> hello() {
        return ResponseEntity.ok(Map.of(
                "message", "Welcome to Tournament Service",
                "instanceId", applicationName + ":" + serverPort
        ));
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
                "service", "Tournament Service",
                "serviceId", applicationName.toUpperCase(),
                "instanceId", applicationName + ":" + serverPort,
                "version", "1.0",
                "status", "UP"
        ));
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> statistics() {
        Map<String, Object> stats = new LinkedHashMap<>(tournamentService.getTournamentStatistics());
        stats.put("totalCompetitions", competitionService.getAllCompetitions().size());
        stats.put("totalSports", competitionService.getAllSports().size());
        stats.put("instanceId", applicationName + ":" + serverPort);
        return ResponseEntity.ok(stats);
    }

    // TH6 + TH7: Composite Dashboard Aggregator (calls match-service and ranking-service via Service Discovery)
    @GetMapping("/{id}/dashboard")
    public ResponseEntity<Map<String, Object>> getTournamentDashboard(@PathVariable Long id) {
        Tournament tournament = tournamentService.getTournamentById(id);
        if (tournament == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournament not found with id: " + id);
        }

        List<Competition> competitions = competitionService.getCompetitionsByTournament(id);

        List<Map<String, Object>> matches = Collections.emptyList();
        try {
            matches = matchRestClient.get()
                    .uri(matchServiceUrl + "/api/v1/matches/tournament/{id}", id)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ignored) {
        }

        List<Map<String, Object>> rankings = Collections.emptyList();
        try {
            rankings = rankingRestClient.get()
                    .uri(rankingServiceUrl + "/api/v1/rankings")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ignored) {
        }

        java.util.Set<Long> compIds = competitions == null ? Collections.emptySet()
                : competitions.stream()
                        .map(Competition::getId)
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toSet());
        long tournamentRankingsCount = 0L;
        if (rankings != null && !compIds.isEmpty()) {
            tournamentRankingsCount = rankings.stream()
                    .filter(r -> r.get("competitionId") != null)
                    .filter(r -> {
                        try {
                            return compIds.contains(Long.valueOf(String.valueOf(r.get("competitionId"))));
                        } catch (Exception ex) {
                            return false;
                        }
                    })
                    .count();
        }

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("tournament", tournament);
        dashboard.put("competitions", competitions);
        dashboard.put("matches", matches != null ? matches : Collections.emptyList());
        dashboard.put("totalMatches", matches != null ? matches.size() : 0);
        dashboard.put("rankingsCount", tournamentRankingsCount);
        dashboard.put("handledByInstance", applicationName + ":" + serverPort);
        return ResponseEntity.ok(dashboard);
    }

    // GET ALL + SEARCH + FILTER + SORT + PAGINATION (Public API)
    @GetMapping
    public ResponseEntity<TournamentService.TournamentPage> getAllTournaments(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        return ResponseEntity.ok(
                tournamentService.searchTournaments(
                        name,
                        keyword,
                        status,
                        page,
                        size,
                        sortBy,
                        direction
                )
        );
    }

    // GET BY ID (Public API)
    @GetMapping("/{id}")
    public ResponseEntity<Tournament> getTournamentById(@PathVariable Long id) {
        String instanceId = applicationName + ":" + serverPort;
        System.out.println(">>> [" + instanceId + "] Handling GET /api/v1/tournaments/" + id);
        Tournament tournament = tournamentService.getTournamentById(id);

        if (tournament == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Tournament not found with id: " + id
            );
        }

        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .header("X-Instance-Port", String.valueOf(serverPort))
                .body(tournament);
    }

    // POST (Protected - ROLE_ORGANIZER, ROLE_ADMIN)
    @PostMapping
    public ResponseEntity<Tournament> createTournament(
            @Valid @RequestBody Tournament tournament,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Full authentication is required");
        }
        String caller = authentication.getName();
        boolean isAdmin = hasAdminRole(authentication);

        if (!isAdmin) {
            if (tournament.getOrganizerUsername() != null
                    && !tournament.getOrganizerUsername().isBlank()
                    && !tournament.getOrganizerUsername().trim().equalsIgnoreCase(caller)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Access Denied: Organizer không được đặt organizerUsername thành tài khoản khác"
                );
            }
            tournament.setOrganizerUsername(caller);
        } else if (tournament.getOrganizerUsername() == null || tournament.getOrganizerUsername().isBlank()) {
            tournament.setOrganizerUsername(caller);
        }

        Tournament created = tournamentService.createTournament(tournament);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("X-Instance-Id", applicationName + ":" + serverPort)
                .body(created);
    }

    // PUT (Protected - Resource Ownership: Only Owner or ADMIN)
    @PutMapping("/{id}")
    public ResponseEntity<Tournament> updateTournament(
            @PathVariable Long id,
            @Valid @RequestBody Tournament tournament,
            Authentication authentication) {

        Tournament existing = tournamentService.getTournamentById(id);
        if (existing == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Tournament not found with id: " + id
            );
        }

        checkOwnership(existing, authentication);

        // Non-admin cannot change owner; keep original owner unless Admin changes it explicitly
        if (!hasAdminRole(authentication)
                || tournament.getOrganizerUsername() == null
                || tournament.getOrganizerUsername().isBlank()) {
            tournament.setOrganizerUsername(existing.getOrganizerUsername());
        }

        Tournament updated = tournamentService.updateTournament(id, tournament);
        return ResponseEntity.ok(updated);
    }

    // PATCH (Protected - Resource Ownership: Only Owner or ADMIN)
    @PatchMapping("/{id}")
    public ResponseEntity<Tournament> patchTournament(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates,
            Authentication authentication) {

        Tournament existing = tournamentService.getTournamentById(id);
        if (existing == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Tournament not found with id: " + id
            );
        }

        checkOwnership(existing, authentication);

        if (!hasAdminRole(authentication) && updates != null && updates.containsKey("organizerUsername")) {
            Object requestedOwner = updates.get("organizerUsername");
            String existingOwner = existing.getOrganizerUsername();
            if (requestedOwner != null && !String.valueOf(requestedOwner).isBlank()
                    && (existingOwner == null || !String.valueOf(requestedOwner).trim().equalsIgnoreCase(existingOwner))) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Access Denied: Organizer không được thay đổi organizerUsername"
                );
            }
            updates.remove("organizerUsername");
        }

        Tournament updated = tournamentService.patchTournament(id, updates);
        return ResponseEntity.ok(updated);
    }

    // DELETE (Protected - Resource Ownership: Only Owner or ADMIN)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTournament(
            @PathVariable Long id,
            Authentication authentication) {

        Tournament existing = tournamentService.getTournamentById(id);
        if (existing == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Tournament not found with id: " + id
            );
        }

        checkOwnership(existing, authentication);

        List<Competition> linkedCompetitions = competitionService.getCompetitionsByTournament(id);
        if (linkedCompetitions != null && !linkedCompetitions.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không thể xóa giải đấu '" + existing.getName() + "' vì đang có " + linkedCompetitions.size() + " nội dung thi đấu liên kết. Vui lòng xóa nội dung thi đấu trước."
            );
        }

        try {
            List<Map<String, Object>> linkedMatches = matchRestClient.get()
                    .uri(matchServiceUrl + "/api/v1/matches/tournament/{id}", id)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            if (linkedMatches != null && !linkedMatches.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Không thể xóa giải đấu '" + existing.getName() + "' vì đang có " + linkedMatches.size() + " trận đấu thuộc giải này."
                );
            }
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Match Service is unavailable when verifying matches before deleting tournament #" + id,
                    ex
            );
        }

        tournamentService.deleteTournament(id);
        return ResponseEntity.noContent().build();
    }

    private boolean hasAdminRole(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private void checkOwnership(Tournament tournament, Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Full authentication is required to modify this resource"
            );
        }

        if (hasAdminRole(authentication)) {
            return; // Admin can modify any tournament
        }

        String owner = tournament.getOrganizerUsername();
        if (owner == null || owner.isBlank() || !owner.equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access Denied: You do not own this tournament"
            );
        }
    }
}
