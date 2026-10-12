package com.sports.management.tournament.repository;

import com.sports.management.tournament.model.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentRepository
        extends JpaRepository<Tournament, Long> {
}