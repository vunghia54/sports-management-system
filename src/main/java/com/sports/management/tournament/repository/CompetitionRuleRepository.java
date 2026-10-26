package com.sports.management.tournament.repository;

import com.sports.management.tournament.model.CompetitionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompetitionRuleRepository extends JpaRepository<CompetitionRule, Long> {
    Optional<CompetitionRule> findByCompetitionId(Long competitionId);
}
