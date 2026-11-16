package com.sports.management.ranking.repository;

import com.sports.management.ranking.model.RankingItem;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public interface RankingItemRepository extends JpaRepository<RankingItem, Long>, JpaSpecificationExecutor<RankingItem> {

    List<RankingItem> findByRankingIdOrderByRankPositionAsc(Long rankingId);

    List<RankingItem> findByRankingIdOrderByPointsDescGoalDifferenceDescGoalsForDesc(Long rankingId);

    default Page<RankingItem> searchItems(
            Long rankingId,
            String keyword,
            Pageable pageable) {
        return findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("rankingId"), rankingId));
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(cb.coalesce(root.get("participantName"), "")), pattern));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    Optional<RankingItem> findByRankingIdAndParticipantName(Long rankingId, String participantName);

    void deleteByRankingId(Long rankingId);
}
