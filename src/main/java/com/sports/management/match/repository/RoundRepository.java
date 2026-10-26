package com.sports.management.match.repository;

import com.sports.management.match.model.Round;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoundRepository extends JpaRepository<Round, Long> {

    List<Round> findByCompetitionIdOrderByRoundOrderAsc(Long competitionId);

}
