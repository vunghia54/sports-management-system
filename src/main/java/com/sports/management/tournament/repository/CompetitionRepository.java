package com.sports.management.tournament.repository;

import com.sports.management.tournament.model.Competition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long> {
    List<Competition> findByTournamentId(Long tournamentId);
}
