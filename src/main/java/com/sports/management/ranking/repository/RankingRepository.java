package com.sports.management.ranking.repository;

import com.sports.management.ranking.model.Ranking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RankingRepository extends JpaRepository<Ranking, Long> {

    Optional<Ranking> findByCompetitionId(Long competitionId);

    boolean existsByCompetitionId(Long competitionId);
}
