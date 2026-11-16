package com.sports.management.result.repository;

import com.sports.management.result.model.RankingSyncTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RankingSyncTaskRepository extends JpaRepository<RankingSyncTask, Long> {

    @Query("""
            SELECT t FROM RankingSyncTask t
            WHERE UPPER(t.status) IN ('PENDING', 'FAILED')
              AND (t.permanentFailure IS NULL OR t.permanentFailure = FALSE)
              AND COALESCE(t.attempts, 0) < :maxAttempts
              AND (t.nextRetryAt IS NULL OR t.nextRetryAt <= :now)
            ORDER BY t.id ASC
            """)
    List<RankingSyncTask> findDueTasks(
            @Param("maxAttempts") int maxAttempts,
            @Param("now") LocalDateTime now);

    List<RankingSyncTask> findByCompetitionIdOrderByIdAsc(Long competitionId);
}
