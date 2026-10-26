package com.sports.management.ranking.repository;

import com.sports.management.ranking.model.RankingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RankingItemRepository extends JpaRepository<RankingItem, Long> {

    List<RankingItem> findByRankingIdOrderByRankPositionAsc(Long rankingId);

    List<RankingItem> findByRankingIdOrderByPointsDescGoalDifferenceDescGoalsForDesc(Long rankingId);

    Optional<RankingItem> findByRankingIdAndParticipantName(Long rankingId, String participantName);

    void deleteByRankingId(Long rankingId);
}
