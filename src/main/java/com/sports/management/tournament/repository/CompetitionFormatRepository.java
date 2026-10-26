package com.sports.management.tournament.repository;

import com.sports.management.tournament.model.CompetitionFormat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompetitionFormatRepository extends JpaRepository<CompetitionFormat, Long> {
    Optional<CompetitionFormat> findByCompetitionId(Long competitionId);
}
