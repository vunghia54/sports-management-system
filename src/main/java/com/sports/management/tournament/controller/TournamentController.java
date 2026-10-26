
package com.sports.management.tournament.controller;

import com.sports.management.tournament.model.Tournament;
import com.sports.management.tournament.service.TournamentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tournaments")
public class TournamentController {

    private final TournamentService tournamentService;

    public TournamentController(
            TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    // GET ALL + SEARCH + FILTER + SORT + PAGINATION
    @GetMapping
    public ResponseEntity<TournamentService.TournamentPage>
    getAllTournaments(
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

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Tournament> getTournamentById(
            @PathVariable Long id) {

        Tournament tournament =
                tournamentService.getTournamentById(id);

        if (tournament == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Tournament not found with id: " + id
            );
        }

        return ResponseEntity.ok(tournament);
    }

    // POST
    @PostMapping
    public ResponseEntity<Tournament> createTournament(
            @Valid @RequestBody Tournament tournament) {

        Tournament created =
                tournamentService.createTournament(tournament);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    // PUT
    @PutMapping("/{id}")
    public ResponseEntity<Tournament> updateTournament(
            @PathVariable Long id,
            @Valid @RequestBody Tournament tournament) {

        Tournament updated =
                tournamentService.updateTournament(
                        id,
                        tournament
                );

        if (updated == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Tournament not found with id: " + id
            );
        }

        return ResponseEntity.ok(updated);
    }

    // PATCH
    @PatchMapping("/{id}")
    public ResponseEntity<Tournament> patchTournament(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {

        Tournament updated =
                tournamentService.patchTournament(id, updates);

        return ResponseEntity.ok(updated);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTournament(
            @PathVariable Long id) {

        boolean deleted =
                tournamentService.deleteTournament(id);

        if (!deleted) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Tournament not found with id: " + id
            );
        }

        return ResponseEntity.noContent().build();
    }
}
