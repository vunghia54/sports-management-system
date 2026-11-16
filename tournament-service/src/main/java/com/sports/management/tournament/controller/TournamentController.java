package com.sports.management.tournament.controller;

import com.sports.management.tournament.model.Competition;
import com.sports.management.tournament.model.Tournament;
import com.sports.management.tournament.service.CompetitionService;
import com.sports.management.tournament.service.TournamentService;
import jakarta.validation.Valid;
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
    private final RestClient loadBalancedRestClient;

    @Value("${spring.application.name:tournament-service}")
    private String applicationName;

    @Value("${server.port:8081}")
    private int serverPort;

    public TournamentController(
            TournamentService tournamentService,
            CompetitionService competitionService,
            @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder) {
        this.tournamentService = tournamentService;
        this.competitionService = competitionService;
        this.loadBalancedRestClient = loadBalancedRestClientBuilder.build();
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
            matches = loadBalancedRestClient.get()
                    .uri("http://match-service/api/v1/matches/tournament/{id}", id)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ignored) {
        }

        List<Map<String, Object>> rankings = Collections.emptyList();
        try {
            rankings = loadBalancedRestClient.get()
                    .uri("http://ranking-service/api/v1/rankings")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ignored) {
        }

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("tournament", tournament);
        dashboard.put("competitions", competitions);
        dashboard.put("matches", matches != null ? matches : Collections.emptyList());
        dashboard.put("totalMatches", matches != null ? matches.size() : 0);
        dashboard.put("rankingsCount", rankings != null ? rankings.size() : 0);
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

        String creator = (authentication != null && authentication.getName() != null)
                ? authentication.getName()
                : "organizer1";

        if (tournament.getOrganizerUsername() == null || tournament.getOrganizerUsername().isBlank()) {
            tournament.setOrganizerUsername(creator);
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

        // Keep original owner if not specified
        if (tournament.getOrganizerUsername() == null || tournament.getOrganizerUsername().isBlank()) {
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
            List<Map<String, Object>> linkedMatches = loadBalancedRestClient.get()
                    .uri("http://match-service/api/v1/matches/tournament/{id}", id)
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
        } catch (Exception ignored) {
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
        if (owner != null && !owner.equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access Denied: You do not own this tournament"
            );
        }
    }
}
