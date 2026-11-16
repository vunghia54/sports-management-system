package com.sports.management.ranking.repository;

import com.sports.management.ranking.model.Ranking;
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
public interface RankingRepository extends JpaRepository<Ranking, Long>, JpaSpecificationExecutor<Ranking> {

    Optional<Ranking> findByCompetitionId(Long competitionId);

    default Page<Ranking> searchRankings(
            Long competitionId,
            String keyword,
            Pageable pageable) {
        return findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (competitionId != null) {
                predicates.add(cb.equal(root.get("competitionId"), competitionId));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(cb.coalesce(root.get("rankingName"), "")), pattern));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }
}
