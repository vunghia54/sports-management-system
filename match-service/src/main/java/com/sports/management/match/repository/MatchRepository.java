package com.sports.management.match.repository;

import com.sports.management.match.model.Match;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public interface MatchRepository extends JpaRepository<Match, Long>, JpaSpecificationExecutor<Match> {

    List<Match> findByTournamentId(Long tournamentId);

    List<Match> findByCompetitionId(Long competitionId);

    boolean existsByParticipant1IdOrParticipant2Id(Long participant1Id, Long participant2Id);

    long countByStatusIgnoreCase(String status);

    default Page<Match> searchMatches(
            Long tournamentId,
            Long competitionId,
            String status,
            String keyword,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay,
            Pageable pageable) {
        return findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (tournamentId != null) {
                predicates.add(cb.equal(root.get("tournamentId"), tournamentId));
            }
            if (competitionId != null) {
                predicates.add(cb.equal(root.get("competitionId"), competitionId));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.lower(cb.coalesce(root.get("status"), "")), status.trim().toLowerCase(Locale.ROOT)));
            }
            if (startOfDay != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("matchTime"), startOfDay));
            }
            if (endOfDay != null) {
                predicates.add(cb.lessThan(root.get("matchTime"), endOfDay));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("participant1"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("participant2"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("location"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("round"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("referee"), "")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }
}
