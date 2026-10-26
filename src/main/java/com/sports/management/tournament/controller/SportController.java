package com.sports.management.tournament.controller;

import com.sports.management.tournament.model.Sport;
import com.sports.management.tournament.service.CompetitionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public ResponseEntity<Sport> createSport(@Valid @RequestBody Sport sport) {
        Sport created = competitionService.createSport(sport);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
