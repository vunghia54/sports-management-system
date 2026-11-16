package com.sports.management.tournament.controller;

import com.sports.management.tournament.model.Sport;
import com.sports.management.tournament.service.CompetitionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sports")
public class SportController {

    private final CompetitionService competitionService;

    public SportController(CompetitionService competitionService) {
        this.competitionService = competitionService;
    }

    @GetMapping
    public ResponseEntity<List<Sport>> getAllSports() {
        return ResponseEntity.ok(competitionService.getAllSports());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Sport> getSportById(@PathVariable Long id) {
        Sport sport = competitionService.getSportById(id);
        if (sport == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sport not found with id: " + id);
        }
        return ResponseEntity.ok(sport);
    }

    @PostMapping
    public ResponseEntity<Sport> createSport(@Valid @RequestBody Sport sport) {
        Sport created = competitionService.createSport(sport);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Sport> updateSport(@PathVariable Long id, @Valid @RequestBody Sport sport) {
        Sport updated = competitionService.updateSport(id, sport);
        if (updated == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sport not found with id: " + id);
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSport(@PathVariable Long id) {
        if (!competitionService.deleteSport(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sport not found with id: " + id);
        }
        return ResponseEntity.noContent().build();
    }
}

