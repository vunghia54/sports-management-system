package com.sports.management.athlete.controller;

import com.sports.management.athlete.client.TeamAndMatchClient;
import com.sports.management.athlete.model.Athlete;
import com.sports.management.athlete.model.AthleteResponse;
import com.sports.management.athlete.service.AthleteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/athletes")
public class AthleteController {

    private final AthleteService athleteService;
    private final TeamAndMatchClient teamAndMatchClient;

    @Value("${spring.application.name:athlete-service}")
    private String applicationName;

    @Value("${server.port:8086}")
    private int serverPort;

    public AthleteController(AthleteService athleteService) {
        this(athleteService, null);
    }

    @Autowired
    public AthleteController(AthleteService athleteService, TeamAndMatchClient teamAndMatchClient) {
        this.athleteService = athleteService;
        this.teamAndMatchClient = teamAndMatchClient;
    }

    // TH1: Hello, Info, Statistics
    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> hello() {
        return ResponseEntity.ok(Map.of(
                "message", "Welcome to Athlete Service",
                "instanceId", applicationName + ":" + serverPort
        ));
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
                "service", "Athlete Service",
                "serviceId", applicationName.toUpperCase(),
                "instanceId", applicationName + ":" + serverPort,
                "version", "1.0",
                "status", "UP"
        ));
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> statistics() {
        return ResponseEntity.ok(athleteService.getStatistics());
    }

    // TH3: Paged Search / Filter / Sort
    @GetMapping("/paged")
    public ResponseEntity<AthleteService.AthletePage> getAthletesPaged(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String healthStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            Authentication authentication) {
        return ResponseEntity.ok(athleteService.searchAthletesPaged(
                name, keyword, gender, healthStatus, page, size, sortBy, direction, authentication));
    }

    // GET ALL + SEARCH + FILTER + SORT
    @GetMapping
    public ResponseEntity<List<AthleteResponse>> getAllAthletes(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String healthStatus,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            Authentication authentication) {
        return ResponseEntity.ok(athleteService.filterAthleteResponses(
                name, keyword, gender, healthStatus, sortBy, direction, authentication));
    }

    // PROTECTED CCCD LOOKUP (ADMIN, ORGANIZER, or owning TEAM_MANAGER)
    @GetMapping({"/cccd/{identityCardNumber}", "/search/cccd/{identityCardNumber}"})
    public ResponseEntity<AthleteResponse> getAthleteByCccd(
            @PathVariable String identityCardNumber,
            Authentication authentication) {
        return ResponseEntity.ok(athleteService.findByIdentityCardNumberAuthorized(identityCardNumber, authentication));
    }

    // GET BY ID (with Instance Header for TH7 Service Discovery + PII Redaction)
    @GetMapping("/{id}")
    public ResponseEntity<AthleteResponse> getAthleteById(
            @PathVariable Long id,
            Authentication authentication) {
        Athlete athlete = athleteService.getAthleteById(id);
        if (athlete == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found with id: " + id);
        }
        String instanceId = applicationName + ":" + serverPort;
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .header("X-Instance-Port", String.valueOf(serverPort))
                .body(athleteService.toProjectedResponse(athlete, authentication));
    }

    @PostMapping
    public ResponseEntity<AthleteResponse> createAthlete(
            @Valid @RequestBody Athlete athlete,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Full authentication is required");
        }
        String caller = authentication.getName().trim().toLowerCase(java.util.Locale.ROOT);
        boolean isAdmin = hasAdminRole(authentication);
        if (!isAdmin) {
            if (athlete.getOwnerUsername() != null && !athlete.getOwnerUsername().isBlank()
                    && !athlete.getOwnerUsername().trim().equalsIgnoreCase(caller)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Access Denied: Bạn không được đặt ownerUsername thành tài khoản khác");
            }
            athlete.setOwnerUsername(caller);
        } else if (athlete.getOwnerUsername() == null || athlete.getOwnerUsername().isBlank()) {
            athlete.setOwnerUsername(caller);
        } else {
            athlete.setOwnerUsername(athlete.getOwnerUsername().trim().toLowerCase(java.util.Locale.ROOT));
        }
        Athlete created = athleteService.createAthlete(athlete);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Instance-Id", applicationName + ":" + serverPort)
                .body(athleteService.toProjectedResponse(created, authentication));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AthleteResponse> updateAthlete(
            @PathVariable Long id,
            @Valid @RequestBody Athlete athlete,
            Authentication authentication) {
        Athlete existing = athleteService.getAthleteById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found with id: " + id);
        }
        checkOwnership(existing, authentication);
        if (!hasAdminRole(authentication)
                || athlete.getOwnerUsername() == null
                || athlete.getOwnerUsername().isBlank()) {
            athlete.setOwnerUsername(existing.getOwnerUsername());
        }
        Athlete updated = athleteService.updateAthlete(id, athlete);
        return ResponseEntity.ok(athleteService.toProjectedResponse(updated, authentication));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AthleteResponse> patchAthlete(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates,
            Authentication authentication) {
        Athlete existing = athleteService.getAthleteById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found with id: " + id);
        }
        checkOwnership(existing, authentication);
        if (!hasAdminRole(authentication) && updates != null && updates.containsKey("ownerUsername")) {
            Object reqOwner = updates.get("ownerUsername");
            if (reqOwner != null && !String.valueOf(reqOwner).isBlank()
                    && (existing.getOwnerUsername() == null
                    || !String.valueOf(reqOwner).trim().equalsIgnoreCase(existing.getOwnerUsername()))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Access Denied: Bạn không được thay đổi ownerUsername của VĐV");
            }
            updates.remove("ownerUsername");
        }
        Athlete updated = athleteService.patchAthlete(id, updates);
        return ResponseEntity.ok(athleteService.toProjectedResponse(updated, authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAthlete(
            @PathVariable Long id,
            Authentication authentication) {
        Athlete existing = athleteService.getAthleteById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found with id: " + id);
        }
        checkOwnership(existing, authentication);
        if (teamAndMatchClient != null) {
            if (teamAndMatchClient.isAthleteUsedInTeams(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Không thể xóa VĐV #" + id + " vì VĐV vẫn đang thuộc biên chế đội tuyển.");
            }
            if (teamAndMatchClient.isAthleteUsedInMatches(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Không thể xóa VĐV #" + id + " vì VĐV đang có hồ sơ đăng ký hoặc tham gia giải đấu.");
            }
        }
        athleteService.deleteAthlete(id);
        return ResponseEntity.noContent().build();
    }

    private boolean hasAdminRole(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()));
    }

    private void checkOwnership(Athlete athlete, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Full authentication is required");
        }
        boolean privileged = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority())
                        || "ROLE_ORGANIZER".equalsIgnoreCase(a.getAuthority()));
        if (privileged) {
            return;
        }
        String owner = athlete.getOwnerUsername();
        String caller = authentication.getName().trim();
        if (owner == null || owner.isBlank() || !owner.trim().equalsIgnoreCase(caller)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not own this athlete profile");
        }
    }
}
