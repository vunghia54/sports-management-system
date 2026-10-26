package com.sports.management.tournament.controller;

import com.sports.management.tournament.model.Competition;
import com.sports.management.tournament.model.CompetitionFormat;
import com.sports.management.tournament.model.CompetitionRule;
import com.sports.management.tournament.model.Sport;
import com.sports.management.tournament.service.CompetitionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/competitions")
public class CompetitionController {

    private final CompetitionService competitionService;

    public CompetitionController(CompetitionService competitionService) {
        this.competitionService = competitionService;
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
        return ResponseEntity.ok(competition);
    }

    @PostMapping
    public ResponseEntity<Competition> createCompetition(@Valid @RequestBody Competition competition) {
        Competition created = competitionService.createCompetition(competition);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}/rule")
    public ResponseEntity<CompetitionRule> getCompetitionRule(@PathVariable Long id) {
        CompetitionRule rule = competitionService.getRuleByCompetition(id);
        if (rule == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rule not found for competition: " + id);
        }
        return ResponseEntity.ok(rule);
    }

    @GetMapping("/{id}/format")
    public ResponseEntity<CompetitionFormat> getCompetitionFormat(@PathVariable Long id) {
        CompetitionFormat format = competitionService.getFormatByCompetition(id);
        if (format == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Format not found for competition: " + id);
        }
        return ResponseEntity.ok(format);
    }
}
