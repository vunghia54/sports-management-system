package com.sports.management.athlete.controller;

import com.sports.management.athlete.model.Athlete;
import com.sports.management.athlete.service.AthleteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/athletes")
public class AthleteController {

    private final AthleteService athleteService;

    public AthleteController(AthleteService athleteService) {
        this.athleteService = athleteService;
    }

    @GetMapping
    public ResponseEntity<List<Athlete>> getAllAthletes() {
        return ResponseEntity.ok(athleteService.getAllAthletes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Athlete> getAthleteById(@PathVariable Long id) {
        Athlete athlete = athleteService.getAthleteById(id);
        if (athlete == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found with id: " + id);
        }
        return ResponseEntity.ok(athlete);
    }

    @PostMapping
    public ResponseEntity<Athlete> createAthlete(@Valid @RequestBody Athlete athlete) {
        Athlete created = athleteService.createAthlete(athlete);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
