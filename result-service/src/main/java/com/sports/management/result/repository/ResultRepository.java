package com.sports.management.result.repository;

import com.sports.management.result.model.Result;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long>, JpaSpecificationExecutor<Result> {

    Optional<Result> findByMatchId(Long matchId);

    List<Result> findByMatchIdIn(Collection<Long> matchIds);

    boolean existsByMatchId(Long matchId);

    long countByStatusIgnoreCase(String status);

    long countBySyncStatusIgnoreCase(String syncStatus);

    @Query("""
            SELECT r FROM Result r
            WHERE UPPER(r.status) = 'CONFIRMED'
              AND UPPER(COALESCE(r.syncStatus, 'PENDING')) IN ('PENDING', 'FAILED')
              AND (r.syncPermanentFailure IS NULL OR r.syncPermanentFailure = FALSE)
              AND COALESCE(r.syncAttempts, 0) < :maxAttempts
              AND (r.nextRetryAt IS NULL OR r.nextRetryAt <= :now)
              AND (r.syncLeaseUntil IS NULL OR r.syncLeaseUntil <= :now)
            ORDER BY r.id ASC
            """)
    List<Result> findPendingOrRetryableSyncJobs(
            @Param("maxAttempts") int maxAttempts,
            @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Result r
            SET r.syncClaimId = :claimId,
                r.syncLeaseUntil = :leaseUntil
            WHERE r.id = :id
              AND COALESCE(r.resultVersion, 1L) = :expectedVersion
              AND UPPER(r.status) = 'CONFIRMED'
              AND UPPER(COALESCE(r.syncStatus, 'PENDING')) <> 'SYNCED'
              AND (r.syncLeaseUntil IS NULL OR r.syncLeaseUntil < :now)
            """)
    int claimSyncJob(
            @Param("id") Long id,
            @Param("expectedVersion") Long expectedVersion,
            @Param("claimId") String claimId,
            @Param("leaseUntil") LocalDateTime leaseUntil,
            @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Result r
            SET r.competitionId = :competitionId
            WHERE r.id = :id
              AND COALESCE(r.resultVersion, 1L) = :expectedVersion
              AND r.syncClaimId = :claimId
              AND UPPER(r.status) = 'CONFIRMED'
            """)
    int updateCompetitionIdIfVersion(
            @Param("id") Long id,
            @Param("expectedVersion") Long expectedVersion,
            @Param("claimId") String claimId,
            @Param("competitionId") Long competitionId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Result r
            SET r.matchSynced = TRUE,
                r.lastSyncAttemptAt = :now
            WHERE r.id = :id
              AND COALESCE(r.resultVersion, 1L) = :expectedVersion
              AND r.syncClaimId = :claimId
              AND UPPER(r.status) = 'CONFIRMED'
            """)
    int markMatchSyncedIfVersion(
            @Param("id") Long id,
            @Param("expectedVersion") Long expectedVersion,
            @Param("claimId") String claimId,
            @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Result r
            SET r.matchSynced = TRUE,
                r.rankingSynced = TRUE,
                r.syncStatus = 'SYNCED',
                r.syncError = NULL,
                r.syncPermanentFailure = FALSE,
                r.nextRetryAt = NULL,
                r.syncAttempts = COALESCE(r.syncAttempts, 0) + 1,
                r.lastSyncAttemptAt = :now,
                r.syncClaimId = NULL,
                r.syncLeaseUntil = NULL
            WHERE r.id = :id
              AND COALESCE(r.resultVersion, 1L) = :expectedVersion
              AND r.syncClaimId = :claimId
              AND UPPER(r.status) = 'CONFIRMED'
            """)
    int markAllSyncedIfVersion(
            @Param("id") Long id,
            @Param("expectedVersion") Long expectedVersion,
            @Param("claimId") String claimId,
            @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Result r
            SET r.syncStatus = 'FAILED',
                r.syncError = :error,
                r.syncPermanentFailure = :permanent,
                r.nextRetryAt = :nextRetryAt,
                r.syncAttempts = COALESCE(r.syncAttempts, 0) + 1,
                r.lastSyncAttemptAt = :now,
                r.syncClaimId = NULL,
                r.syncLeaseUntil = NULL
            WHERE r.id = :id
              AND COALESCE(r.resultVersion, 1L) = :expectedVersion
              AND r.syncClaimId = :claimId
              AND UPPER(r.status) = 'CONFIRMED'
            """)
    int markSyncFailedIfVersion(
            @Param("id") Long id,
            @Param("expectedVersion") Long expectedVersion,
            @Param("claimId") String claimId,
            @Param("error") String error,
            @Param("permanent") boolean permanent,
            @Param("nextRetryAt") LocalDateTime nextRetryAt,
            @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Result r
            SET r.syncClaimId = NULL,
                r.syncLeaseUntil = NULL
            WHERE r.id = :id
              AND r.syncClaimId = :claimId
            """)
    int releaseSyncClaim(
            @Param("id") Long id,
            @Param("claimId") String claimId);

    default Page<Result> searchResults(
            Long matchId,
            String status,
            String winner,
            Pageable pageable) {
        return findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (matchId != null) {
                predicates.add(cb.equal(root.get("matchId"), matchId));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.lower(cb.coalesce(root.get("status"), "")), status.trim().toLowerCase(Locale.ROOT)));
            }
            if (winner != null && !winner.isBlank()) {
                String pattern = "%" + winner.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(cb.coalesce(root.get("winner"), "")), pattern));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }
}
