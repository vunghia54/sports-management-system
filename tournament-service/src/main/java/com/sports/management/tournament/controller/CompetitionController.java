package com.sports.management.tournament.controller;

import com.sports.management.tournament.model.Competition;
import com.sports.management.tournament.model.CompetitionFormat;
import com.sports.management.tournament.model.CompetitionRule;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/competitions")
public class CompetitionController {

    private final CompetitionService competitionService;
    private final TournamentService tournamentService;
    private final RestClient matchRestClient;
    private final String matchServiceUrl;

    @Value("${spring.application.name:tournament-service}")
    private String applicationName;

    @Value("${server.port:8081}")
    private int serverPort;

    public CompetitionController(
            CompetitionService competitionService,
            TournamentService tournamentService,
            @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder) {
        this(competitionService, tournamentService, loadBalancedRestClientBuilder, "http://match-service");
    }

    @Autowired
    public CompetitionController(
            CompetitionService competitionService,
            TournamentService tournamentService,
            @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
            @Value("${services.match.url:http://match-service}") String matchServiceUrl) {
        this.competitionService = java.util.Objects.requireNonNull(competitionService, "competitionService is required");
        this.tournamentService = java.util.Objects.requireNonNull(tournamentService, "tournamentService is required");
        String candidate = (matchServiceUrl == null || matchServiceUrl.isBlank()) ? "http://match-service" : matchServiceUrl.trim();
        this.matchServiceUrl = candidate.endsWith("/") ? candidate.substring(0, candidate.length() - 1) : candidate;
        if (this.matchServiceUrl.startsWith("http://localhost") || this.matchServiceUrl.startsWith("http://127.0.0.1")) {
            this.matchRestClient = RestClient.builder().build();
        } else {
            this.matchRestClient = loadBalancedRestClientBuilder != null ? loadBalancedRestClientBuilder.build() : null;
        }
    }

    @GetMapping
    public ResponseEntity<List<Competition>> getAllCompetitions(
            @RequestParam(required = false) Long tournamentId) {
        if (tournamentId != null) {
            return ResponseEntity.ok(competitionService.getCompetitionsByTournament(tournamentId));
        }
        return ResponseEntity.ok(competitionService.getAllCompetitions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Competition> getCompetitionById(@PathVariable Long id) {
        Competition competition = competitionService.getCompetitionById(id);
        if (competition == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found with id: " + id);
        }
        String instanceId = applicationName + ":" + serverPort;
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(competition);
    }

    @PostMapping
    public ResponseEntity<Competition> createCompetition(
            @Valid @RequestBody Competition competition,
            Authentication authentication) {
        assertTournamentOwnership(competition.getTournamentId(), authentication);
        Competition created = competitionService.createCompetition(competition);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Competition> updateCompetition(
            @PathVariable Long id,
            @Valid @RequestBody Competition competition,
            Authentication authentication) {
        Competition existing = competitionService.getCompetitionById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found with id: " + id);
        }
        assertTournamentOwnership(existing.getTournamentId(), authentication);
        if (competition.getTournamentId() != null && !competition.getTournamentId().equals(existing.getTournamentId())) {
            assertTournamentOwnership(competition.getTournamentId(), authentication);
        }
        Competition updated = competitionService.updateCompetition(id, competition);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompetition(
            @PathVariable Long id,
            Authentication authentication) {
        Competition existing = competitionService.getCompetitionById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found with id: " + id);
        }
        assertTournamentOwnership(existing.getTournamentId(), authentication);
        assertNoLinkedRegistrationsOrMatches(id);

        if (!competitionService.deleteCompetition(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found with id: " + id);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/rule")
    public ResponseEntity<CompetitionRule> getCompetitionRule(@PathVariable Long id) {
        CompetitionRule rule = competitionService.getRuleByCompetition(id);
        if (rule == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rule not found for competition: " + id);
        }
        return ResponseEntity.ok(rule);
    }

    @PutMapping("/{id}/rule")
    public ResponseEntity<CompetitionRule> updateCompetitionRule(
            @PathVariable Long id,
            @RequestBody CompetitionRule rule,
            Authentication authentication) {
        Competition existing = competitionService.getCompetitionById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found with id: " + id);
        }
        assertTournamentOwnership(existing.getTournamentId(), authentication);
        assertNoMatchesForCompetition(id, "thay đổi luật thi đấu");
        CompetitionRule updated = competitionService.updateRule(id, rule);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/format")
    public ResponseEntity<CompetitionFormat> getCompetitionFormat(@PathVariable Long id) {
        CompetitionFormat format = competitionService.getFormatByCompetition(id);
        if (format == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Format not found for competition: " + id);
        }
        return ResponseEntity.ok(format);
    }

    @PutMapping("/{id}/format")
    public ResponseEntity<CompetitionFormat> updateCompetitionFormat(
            @PathVariable Long id,
            @RequestBody CompetitionFormat format,
            Authentication authentication) {
        Competition existing = competitionService.getCompetitionById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found with id: " + id);
        }
        assertTournamentOwnership(existing.getTournamentId(), authentication);
        assertNoMatchesForCompetition(id, "thay đổi thể thức thi đấu");
        CompetitionFormat updated = competitionService.updateFormat(id, format);
        return ResponseEntity.ok(updated);
    }

    private void assertTournamentOwnership(Long tournamentId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Full authentication is required");
        }
        Tournament tour = tournamentService.getTournamentById(tournamentId);
        if (tour == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tournament not found with id: " + tournamentId);
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()));
        if (isAdmin) {
            return;
        }
        String owner = tour.getOrganizerUsername();
        if (owner == null || owner.isBlank() || !owner.equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access Denied: Bạn không phải ban tổ chức sở hữu giải đấu #" + tournamentId);
        }
    }

    private void assertNoMatchesForCompetition(Long competitionId, String actionLabel) {
        if (matchRestClient == null) {
            return;
        }
        try {
            List<Map<String, Object>> matches = matchRestClient.get()
                    .uri(matchServiceUrl + "/api/v1/matches/competition/{id}", competitionId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            if (matches != null && !matches.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Không thể " + actionLabel + " cho nội dung thi đấu #" + competitionId
                                + " vì đã có " + matches.size() + " trận đấu liên kết.");
            }
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Match Service is unavailable when verifying matches for competition #" + competitionId,
                    ex
            );
        }
    }

    private void assertNoLinkedRegistrationsOrMatches(Long competitionId) {
        if (matchRestClient == null) {
            return;
        }
        assertNoMatchesForCompetition(competitionId, "xóa nội dung thi đấu");
        try {
            List<Map<String, Object>> regs = matchRestClient.get()
                    .uri(matchServiceUrl + "/api/v1/registrations/competition/{id}", competitionId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            if (regs != null && !regs.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Không thể xóa nội dung thi đấu #" + competitionId + " vì đang có " + regs.size() + " hồ sơ đăng ký liên kết.");
            }
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Match Service is unavailable when verifying registrations/matches for competition #" + competitionId,
                    ex
            );
        }
    }
}

