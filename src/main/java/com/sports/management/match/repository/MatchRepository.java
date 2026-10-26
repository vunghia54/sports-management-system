
package com.sports.management.match.repository;

import com.sports.management.match.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByTournamentId(Long tournamentId);

    List<Match> findByCompetitionId(Long competitionId);

}