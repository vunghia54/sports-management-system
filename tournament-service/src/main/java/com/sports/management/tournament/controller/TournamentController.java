package com.sports.management.tournament.controller;

import com.sports.management.tournament.model.Tournament;
import com.sports.management.tournament.service.TournamentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tournaments")
public class TournamentController {

    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    // GET ALL + SEARCH + FILTER + SORT + PAGINATION (Public API)
    @GetMapping
    public ResponseEntity<TournamentService.TournamentPage> getAllTournaments(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        return ResponseEntity.ok(
                tournamentService.searchTournaments(
                        name,
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
        Tournament tournament = tournamentService.getTournamentById(id);

        if (tournament == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Tournament not found with id: " + id
            );
        }

        return ResponseEntity.ok(tournament);
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
