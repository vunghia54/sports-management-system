package com.sports.management.result.service;

import com.sports.management.exception.PermanentSyncException;
import com.sports.management.exception.ResourceNotFoundException;
import com.sports.management.exception.TransientSyncException;
import com.sports.management.result.client.MatchAndRankingClient;
import com.sports.management.result.model.RecordResultRequest;
import com.sports.management.result.model.Result;
import com.sports.management.result.model.ResultDetail;
import com.sports.management.result.model.ResultResponse;
import com.sports.management.result.repository.ResultDetailRepository;
import com.sports.management.result.repository.ResultRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ResultService {

    public static final int MAX_AUTO_SYNC_ATTEMPTS = 5;

    private final ResultRepository resultRepository;
    private final ResultDetailRepository resultDetailRepository;
    private final ResultPersistenceService persistenceService;
    private final MatchAndRankingClient matchAndRankingClient;

    public ResultService(
            ResultRepository resultRepository,
            ResultDetailRepository resultDetailRepository,
            ResultPersistenceService persistenceService,
            MatchAndRankingClient matchAndRankingClient) {
        this.resultRepository = resultRepository;
        this.resultDetailRepository = resultDetailRepository;
        this.persistenceService = persistenceService;
        this.matchAndRankingClient = matchAndRankingClient;
    }

    public List<Result> getAllResults() {
        return resultRepository.findAll();
    }

    public List<Result> getResultsByMatchIds(Collection<Long> matchIds) {
        if (matchIds == null || matchIds.isEmpty()) {
            return Collections.emptyList();
        }
        return resultRepository.findByMatchIdIn(matchIds);
    }

    public org.springframework.data.domain.Page<Result> searchResults(
            Long matchId,
            String status,
            String winner,
            org.springframework.data.domain.Pageable pageable) {
        String safeStatus = (status != null && !status.isBlank()) ? status.trim() : null;
        String safeWinner = (winner != null && !winner.isBlank()) ? winner.trim() : null;
        return resultRepository.searchResults(matchId, safeStatus, safeWinner, pageable);
    }

    public Map<String, Object> getResultStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalResults", resultRepository.count());
        stats.put("confirmedResults", resultRepository.countByStatusIgnoreCase("CONFIRMED"));
        stats.put("pendingResults", resultRepository.countByStatusIgnoreCase("PENDING_CONFIRMATION"));
        stats.put("pendingSyncResults", resultRepository.countBySyncStatusIgnoreCase("PENDING"));
        stats.put("failedSyncResults", resultRepository.countBySyncStatusIgnoreCase("FAILED"));
        return stats;
    }

    public Result getResultById(Long id) {
        return resultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found with id: " + id));
    }

    public List<Result> getPendingOrFailedSyncResults() {
        return resultRepository.findAll().stream()
                .filter(r -> "CONFIRMED".equalsIgnoreCase(r.getStatus()))
                .filter(r -> !"SYNCED".equalsIgnoreCase(r.getSyncStatus()))
                .toList();
    }

    public ResultResponse getResultResponseByMatchId(Long matchId) {
        Result result = resultRepository.findByMatchId(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found for match id: " + matchId));
        List<ResultDetail> details = resultDetailRepository.findByResultId(result.getId());
        return new ResultResponse(result, details);
    }

    public ResultResponse toResultResponse(Result result) {
        List<ResultDetail> details = resultDetailRepository.findByResultId(result.getId());
        return new ResultResponse(result, details);
    }

    public ResultResponse recordResult(RecordResultRequest request, Map<String, Object> verifiedMatch) {
        int s1 = request.getScoreParticipant1() != null ? request.getScoreParticipant1() : 0;
        int s2 = request.getScoreParticipant2() != null ? request.getScoreParticipant2() : 0;
        if (s1 < 0 || s2 < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tỷ số thi đấu không được âm");
        }

        Map<String, Object> match = verifiedMatch != null
                ? verifiedMatch
                : matchAndRankingClient.getMatchById(request.getMatchId());

        String participant1 = match != null && match.get("participant1") != null
                ? String.valueOf(match.get("participant1")) : "PARTICIPANT_1";
        String participant2 = match != null && match.get("participant2") != null
                ? String.valueOf(match.get("participant2")) : "PARTICIPANT_2";
        Long competitionId = extractCompetitionId(match);

        String winner;
        if (s1 > s2) {
            winner = participant1;
        } else if (s2 > s1) {
            winner = participant2;
        } else {
            winner = "DRAW";
        }

        return persistenceService.saveRecordedResult(
                request.getMatchId(),
                s1,
                s2,
                winner,
                request.getNotes(),
                competitionId,
                request.getDetails()
        );
    }

    public ResultResponse confirmResult(
            Long id,
            String confirmedBy,
            String callerBearerToken,
            Map<String, Object> verifiedMatch) {

        Result existing = getResultById(id);
        Long competitionId = extractCompetitionId(verifiedMatch);
        if (competitionId == null) {
            competitionId = existing.getCompetitionId();
        }
        if (competitionId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trận đấu #" + existing.getMatchId()
                            + " chưa gắn nội dung thi đấu (competitionId), không thể xác nhận kết quả và tính bảng xếp hạng."
            );
        }

        // Step 1: Commit CONFIRMED + new resultVersion + syncStatus=PENDING via separate @Transactional bean
        ResultResponse committed = persistenceService.confirmResultInDb(id, confirmedBy, competitionId);
        if ("SYNCED".equalsIgnoreCase(committed.getResult().getSyncStatus())) {
            return committed;
        }

        // Step 2: Execute downstream sync AFTER DB transaction has committed
        return executeSyncPipeline(committed.getResult().getId(), callerBearerToken);
    }

    public SyncAttemptOutcome retrySync(Long id, String callerBearerToken) {
        Result existing = getResultById(id);
        if (!"CONFIRMED".equalsIgnoreCase(existing.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ có thể đồng bộ lại cho kết quả đã ở trạng thái CONFIRMED."
            );
        }
        if ("SYNCED".equalsIgnoreCase(existing.getSyncStatus())) {
            return new SyncAttemptOutcome(toResultResponse(existing), false);
        }

        ResultResponse afterRetry = executeSyncPipeline(id, callerBearerToken);
        Result updated = afterRetry.getResult();
        if ("SYNCED".equalsIgnoreCase(updated.getSyncStatus())) {
            return new SyncAttemptOutcome(afterRetry, false);
        }
        // Check if another concurrent request/worker currently holds an active lease on this version
        if (updated.getSyncClaimId() != null
                && updated.getSyncLeaseUntil() != null
                && updated.getSyncLeaseUntil().isAfter(LocalDateTime.now())) {
            return new SyncAttemptOutcome(afterRetry, true);
        }
        HttpStatus status = updated.isSyncPermanentFailure()
                ? (updated.getSyncError() != null && updated.getSyncError().contains("competitionId")
                    ? HttpStatus.UNPROCESSABLE_CONTENT
                    : HttpStatus.FORBIDDEN)
                : HttpStatus.SERVICE_UNAVAILABLE;
        throw new ResponseStatusException(
                status,
                updated.getSyncError() != null ? updated.getSyncError() : "Đồng bộ liên dịch vụ thất bại"
        );
    }

    public ResultResponse executeSyncPipeline(Long resultId, String bearerToken) {
        Result initial = getResultById(resultId);
        if (!"CONFIRMED".equalsIgnoreCase(initial.getStatus())
                || "SYNCED".equalsIgnoreCase(initial.getSyncStatus())) {
            return toResultResponse(initial);
        }

        final long jobVersion = initial.getResultVersion();
        final String claimId = UUID.randomUUID().toString();
        boolean claimed = persistenceService.tryClaimSyncJob(
                resultId,
                jobVersion,
                claimId,
                LocalDateTime.now().plusSeconds(60)
        );
        if (!claimed) {
            // Another worker/request already owns the active lease or the row changed/synced concurrently
            return toResultResponse(getResultById(resultId));
        }

        try {
            Long matchId = initial.getMatchId();
            Long competitionId = initial.getCompetitionId();

            try {
                // Step 0: Resolve competitionId cleanly; NEVER assume SYNCED without ranking recalculation
                if (competitionId == null) {
                    Map<String, Object> match;
                    try {
                        match = matchAndRankingClient.getMatchById(matchId);
                    } catch (ResourceNotFoundException notFound) {
                        throw new PermanentSyncException(
                                404,
                                "Không tìm thấy trận đấu #" + matchId + " khi đồng bộ để xác định competitionId.",
                                notFound
                        );
                    } catch (ResponseStatusException rse) {
                        throw new TransientSyncException(
                                "Lỗi truy vấn thông tin trận #" + matchId + " để xác định competitionId: " + rse.getReason(),
                                rse
                        );
                    }
                    competitionId = extractCompetitionId(match);
                    if (competitionId == null) {
                        throw new PermanentSyncException(
                                422,
                                "Thiếu competitionId trên trận #" + matchId
                                        + ": không thể tính lại bảng xếp hạng. Vui lòng gán nội dung thi đấu cho trận rồi đồng bộ lại.",
                                null
                        );
                    }
                    if (!persistenceService.updateCompetitionIdIfVersion(resultId, jobVersion, claimId, competitionId)) {
                        return toResultResponse(getResultById(resultId));
                    }
                }

                // Step 1: Mark match FINISHED in Match Service
                if (!initial.isMatchSynced()) {
                    matchAndRankingClient.markMatchFinished(matchId, bearerToken);
                    if (!persistenceService.markMatchSyncedIfVersion(resultId, jobVersion, claimId, competitionId)) {
                        return toResultResponse(getResultById(resultId));
                    }
                }

                // Step 2: Recalculate standings in Ranking Service (mandatory before marking SYNCED)
                if (!initial.isRankingSynced()) {
                    matchAndRankingClient.recalculateRanking(competitionId, bearerToken);
                }

                // Only commit SYNCED/rankingSynced=true if resultVersion & claimId still match the active job
                persistenceService.markAllSyncedIfVersion(resultId, jobVersion, claimId, competitionId);
                return toResultResponse(getResultById(resultId));

            } catch (PermanentSyncException ex) {
                persistenceService.markSyncFailedIfVersion(resultId, jobVersion, claimId, ex.getMessage(), true, null);
                return toResultResponse(getResultById(resultId));
            } catch (TransientSyncException ex) {
                int nextAttempt = initial.getSyncAttempts() + 1;
                long backoffSeconds = Math.min(300L, 15L * nextAttempt);
                persistenceService.markSyncFailedIfVersion(
                        resultId,
                        jobVersion,
                        claimId,
                        ex.getMessage(),
                        false,
                        LocalDateTime.now().plusSeconds(backoffSeconds)
                );
                return toResultResponse(getResultById(resultId));
            } catch (ResponseStatusException ex) {
                boolean perm = ex.getStatusCode().is4xxClientError()
                        && ex.getStatusCode().value() != 408
                        && ex.getStatusCode().value() != 429;
                int nextAttempt = initial.getSyncAttempts() + 1;
                LocalDateTime nextRetry = perm ? null : LocalDateTime.now().plusSeconds(Math.min(300L, 15L * nextAttempt));
                persistenceService.markSyncFailedIfVersion(
                        resultId,
                        jobVersion,
                        claimId,
                        ex.getReason() != null ? ex.getReason() : ex.getMessage(),
                        perm,
                        nextRetry
                );
                return toResultResponse(getResultById(resultId));
            }
        } finally {
            persistenceService.releaseClaim(resultId, claimId);
        }
    }

    public Result patchNotesOnly(Long id, Map<String, Object> updates) {
        return persistenceService.patchNotesOnly(id, updates);
    }

    public void deleteResult(Long id) {
        persistenceService.deleteResultById(id);
    }

    private Long extractCompetitionId(Map<String, Object> match) {
        if (match == null || match.get("competitionId") == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(match.get("competitionId")));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public record SyncAttemptOutcome(ResultResponse response, boolean inProgressElsewhere) {
    }
}
