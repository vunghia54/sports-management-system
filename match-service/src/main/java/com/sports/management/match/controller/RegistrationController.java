package com.sports.management.match.controller;

import com.sports.management.match.model.Participant;
import com.sports.management.match.model.Registration;
import com.sports.management.match.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/registrations")
    public ResponseEntity<Registration> register(
            @Valid @RequestBody Registration registration,
            Authentication authentication) {
        String caller = requireAuthenticatedUsername(authentication);
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
        Registration created = registrationService.register(registration, caller, isAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/registrations/statistics")
    public ResponseEntity<Map<String, Object>> getRegistrationStatistics() {
        return ResponseEntity.ok(registrationService.getStatistics());
    }

    @GetMapping("/registrations")
    public ResponseEntity<List<Registration>> getAllRegistrations(
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) Long competitionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        if (tournamentId == null && competitionId == null && status == null && keyword == null) {
            return ResponseEntity.ok(registrationService.getAllRegistrations());
        }
        return ResponseEntity.ok(registrationService.filterRegistrations(tournamentId, competitionId, status, keyword));
    }

    @GetMapping("/registrations/paged")
    public ResponseEntity<RegistrationService.RegistrationPage> getRegistrationsPaged(
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) Long competitionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(registrationService.searchRegistrationsPaged(tournamentId, competitionId, status, keyword, page, size));
    }

    @GetMapping("/registrations/{id}")
    public ResponseEntity<Registration> getRegistrationById(@PathVariable Long id) {
        return ResponseEntity.ok(registrationService.getRegistrationById(id));
    }

    @GetMapping("/registrations/competition/{competitionId}")
    public ResponseEntity<List<Registration>> getRegistrationsByCompetition(@PathVariable Long competitionId) {
        return ResponseEntity.ok(registrationService.getRegistrationsByCompetition(competitionId));
    }

    @GetMapping("/registrations/teams/{teamId}/roster-change-validation")
    public ResponseEntity<Map<String, Object>> validateTeamRosterChange(
            @PathVariable Long teamId,
            @RequestParam(required = false) Long addedAthleteId,
            @RequestParam(required = false) Integer proposedMemberCount) {
        registrationService.validateTeamRosterChange(teamId, addedAthleteId, proposedMemberCount);
        return ResponseEntity.ok(Map.of("valid", true));
    }

    @PutMapping("/registrations/{id}/approve")
    public ResponseEntity<Registration> approveRegistration(
            @PathVariable Long id,
            Authentication authentication) {
        String actor = requireAuthenticatedUsername(authentication);
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
        return ResponseEntity.ok(registrationService.approveRegistration(id, actor, isAdmin));
    }

    @PutMapping("/registrations/{id}/reject")
    public ResponseEntity<Registration> rejectRegistration(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        String actor = requireAuthenticatedUsername(authentication);
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "Registration rejected by organizer";
        return ResponseEntity.ok(registrationService.rejectRegistration(id, reason, actor, isAdmin));
    }

    @PutMapping("/registrations/{id}/withdraw")
    public ResponseEntity<Registration> withdrawRegistration(
            @PathVariable Long id,
            Authentication authentication) {
        String actor = requireAuthenticatedUsername(authentication);
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
        return ResponseEntity.ok(registrationService.withdrawRegistration(id, actor, isAdmin));
    }

    @GetMapping("/participants/competition/{competitionId}")
    public ResponseEntity<List<Participant>> getParticipantsByCompetition(@PathVariable Long competitionId) {
        return ResponseEntity.ok(registrationService.getParticipantsByCompetition(competitionId));
    }

    @GetMapping("/participants/usage")
    public ResponseEntity<Map<String, Object>> getParticipantUsage(
            @RequestParam String participantType,
            @RequestParam Long referenceId) {
        return ResponseEntity.ok(registrationService.getParticipantUsage(participantType, referenceId));
    }

    private String requireAuthenticatedUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Full authentication is required");
        }
        return authentication.getName();
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> role.equalsIgnoreCase(a.getAuthority()));
    }
}

