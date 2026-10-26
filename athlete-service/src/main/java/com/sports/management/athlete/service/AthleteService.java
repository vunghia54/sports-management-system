package com.sports.management.athlete.service;

import com.sports.management.athlete.model.Athlete;
import com.sports.management.athlete.repository.AthleteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AthleteService {

    private final AthleteRepository athleteRepository;

    public AthleteService(AthleteRepository athleteRepository) {
        this.athleteRepository = athleteRepository;
    }

    public List<Athlete> getAllAthletes() {
        return athleteRepository.findAll();
    }

    public Athlete getAthleteById(Long id) {
        return athleteRepository.findById(id).orElse(null);
    }

    public Athlete createAthlete(Athlete athlete) {
        athlete.setId(null);
        return athleteRepository.save(athlete);
    }
}
