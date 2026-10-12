package com.sports.management.tournament.repository;

import com.sports.management.tournament.model.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TournamentRepository
        extends JpaRepository<Tournament, Long>,
        JpaSpecificationExecutor<Tournament> {
}