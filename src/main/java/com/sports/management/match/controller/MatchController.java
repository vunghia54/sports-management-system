
package com.sports.management.match.controller;

import com.sports.management.match.model.Match;
import com.sports.management.match.service.MatchService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    // GET ALL
    @GetMapping
    public ResponseEntity<List<Match>> getAllMatches() {

        return ResponseEntity.ok(
                matchService.getAllMatches()
        );
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Match> getMatchById(
            @PathVariable Long id) {

        Match match = matchService.getMatchById(id);

        if (match == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Match not found with id: " + id
            );
        }

        return ResponseEntity.ok(match);
    }

    // GET MATCHES BY TOURNAMENT
    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<Match>> getMatchesByTournament(
            @PathVariable Long tournamentId) {

        if (!matchService.tournamentExists(tournamentId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Tournament not found with id: " + tournamentId
            );
        }

        return ResponseEntity.ok(
                matchService.getMatchesByTournamentId(tournamentId)
        );
    }

    // CREATE
    @PostMapping
    public ResponseEntity<Match> createMatch(
            @RequestBody Match match) {

        if (match.getTournamentId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tournament ID is required"
            );
        }

        Match createdMatch = matchService.createMatch(match);

        if (createdMatch == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid tournament ID"
            );
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdMatch);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Match> updateMatch(
            @PathVariable Long id,
            @RequestBody Match match) {

        if (matchService.getMatchById(id) == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Match not found with id: " + id
            );
        }

        if (match.getTournamentId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tournament ID is required"
            );
        }

        if (!matchService.tournamentExists(
                match.getTournamentId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid tournament ID"
            );
        }

        Match updatedMatch =
                matchService.updateMatch(id, match);

        return ResponseEntity.ok(updatedMatch);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMatch(
            @PathVariable Long id) {

        boolean deleted = matchService.deleteMatch(id);

        if (!deleted) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Match not found with id: " + id
            );
        }

        return ResponseEntity.noContent().build();
    }
}
