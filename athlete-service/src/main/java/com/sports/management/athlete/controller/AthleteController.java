package com.sports.management.athlete.controller;

import com.sports.management.athlete.model.Athlete;
import com.sports.management.athlete.service.AthleteService;
import jakarta.validation.Valid;
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

    @Value("${spring.application.name:athlete-service}")
    private String applicationName;

    @Value("${server.port:8086}")
    private int serverPort;

    public AthleteController(AthleteService athleteService) {
        this.athleteService = athleteService;
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
            @RequestParam(defaultValue = "asc") String direction) {
        return ResponseEntity.ok(athleteService.searchAthletesPaged(name, keyword, gender, healthStatus, page, size, sortBy, direction));
    }

    // GET ALL + SEARCH + FILTER + SORT
    @GetMapping
    public ResponseEntity<List<Athlete>> getAllAthletes(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String healthStatus,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return ResponseEntity.ok(athleteService.filterAthletes(name, keyword, gender, healthStatus, sortBy, direction));
    }

    // GET BY ID (with Instance Header for TH7 Service Discovery)
    @GetMapping("/{id}")
    public ResponseEntity<Athlete> getAthleteById(@PathVariable Long id) {
        Athlete athlete = athleteService.getAthleteById(id);
        if (athlete == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found with id: " + id);
        }
        String instanceId = applicationName + ":" + serverPort;
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .header("X-Instance-Port", String.valueOf(serverPort))
                .body(athlete);
    }

    @PostMapping
    public ResponseEntity<Athlete> createAthlete(
            @Valid @RequestBody Athlete athlete,
            Authentication authentication) {
        if (authentication != null && authentication.getName() != null
                && (athlete.getOwnerUsername() == null || athlete.getOwnerUsername().isBlank())) {
            athlete.setOwnerUsername(authentication.getName());
        }
        Athlete created = athleteService.createAthlete(athlete);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Instance-Id", applicationName + ":" + serverPort)
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Athlete> updateAthlete(
            @PathVariable Long id,
            @Valid @RequestBody Athlete athlete,
            Authentication authentication) {
        Athlete existing = athleteService.getAthleteById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found with id: " + id);
        }
        checkOwnership(existing, authentication);
        Athlete updated = athleteService.updateAthlete(id, athlete);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Athlete> patchAthlete(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates,
            Authentication authentication) {
        Athlete existing = athleteService.getAthleteById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found with id: " + id);
        }
        checkOwnership(existing, authentication);
        Athlete updated = athleteService.patchAthlete(id, updates);
        return ResponseEntity.ok(updated);
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
        athleteService.deleteAthlete(id);
        return ResponseEntity.noContent().build();
    }

    private void checkOwnership(Athlete athlete, Authentication authentication) {
        if (authentication == null) return;
        boolean privileged = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_ORGANIZER"));
        if (privileged) return;
        if (athlete.getOwnerUsername() != null && !athlete.getOwnerUsername().equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not own this athlete profile");
        }
    }
}
