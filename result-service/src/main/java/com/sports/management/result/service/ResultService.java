package com.sports.management.result.service;

import com.sports.management.exception.PermanentSyncException;
import com.sports.management.exception.ResourceNotFoundException;
import com.sports.management.exception.TransientSyncException;
import com.sports.management.result.client.MatchAndRankingClient;
import com.sports.management.result.model.RankingSyncTask;
import com.sports.management.result.model.RecordResultRequest;
import com.sports.management.result.model.Result;
import com.sports.management.result.model.ResultDetail;
import com.sports.management.result.model.ResultResponse;
import com.sports.management.result.repository.RankingSyncTaskRepository;
import com.sports.management.result.repository.ResultDetailRepository;
import com.sports.management.result.repository.ResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class ResultService {

    public static final int MAX_AUTO_SYNC_ATTEMPTS = 5;

    private final ResultRepository resultRepository;
    private final ResultDetailRepository resultDetailRepository;
    private final ResultPersistenceService persistenceService;
    private final MatchAndRankingClient matchAndRankingClient;
    private final RankingSyncTaskRepository rankingSyncTaskRepository;

    public ResultService(
            ResultRepository resultRepository,
            ResultDetailRepository resultDetailRepository,
            ResultPersistenceService persistenceService,
            MatchAndRankingClient matchAndRankingClient) {
        this(resultRepository, resultDetailRepository, persistenceService, matchAndRankingClient, null);
    }

    @Autowired
    public ResultService(
            ResultRepository resultRepository,
            ResultDetailRepository resultDetailRepository,
            ResultPersistenceService persistenceService,
            MatchAndRankingClient matchAndRankingClient,
            RankingSyncTaskRepository rankingSyncTaskRepository) {
        this.resultRepository = resultRepository;
        this.resultDetailRepository = resultDetailRepository;
        this.persistenceService = persistenceService;
        this.matchAndRankingClient = matchAndRankingClient;
        this.rankingSyncTaskRepository = rankingSyncTaskRepository;
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

    public Optional<Result> findByMatchId(Long matchId) {
        return matchId != null ? resultRepository.findByMatchId(matchId) : Optional.empty();
    }

    public ResultResponse recordResult(RecordResultRequest request, Map<String, Object> verifiedMatch) {
        return recordResult(request, verifiedMatch, null);
    }

    public ResultResponse recordResult(RecordResultRequest request, Map<String, Object> verifiedMatch, String callerBearerToken) {
        int s1 = request.getScoreParticipant1() != null ? request.getScoreParticipant1() : 0;
        int s2 = request.getScoreParticipant2() != null ? request.getScoreParticipant2() : 0;
        Integer pen1 = request.getPenaltyScore1();
        Integer pen2 = request.getPenaltyScore2();
        if (s1 < 0 || s2 < 0 || (pen1 != null && pen1 < 0) || (pen2 != null && pen2 < 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tỷ số thi đấu và điểm luân lưu không được âm");
        }

        Map<String, Object> match = verifiedMatch != null
                ? verifiedMatch
                : matchAndRankingClient.getMatchById(request.getMatchId());

        assertMatchEligibleForResult(match, request.getMatchId());

        String participant1 = match != null && match.get("participant1") != null
                ? String.valueOf(match.get("participant1")) : "PARTICIPANT_1";
        String participant2 = match != null && match.get("participant2") != null
                ? String.valueOf(match.get("participant2")) : "PARTICIPANT_2";
        Long competitionId = extractCompetitionId(match);

        if (s1 == s2 && competitionId != null && isKnockoutCompetition(competitionId)) {
            if (pen1 == null || pen2 == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Nội dung loại trực tiếp (KNOCKOUT) có tỉ số hòa phải nhập tỷ số luân lưu (penaltyScore1, penaltyScore2)");
            }
            if (Objects.equals(pen1, pen2)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Tỷ số luân lưu (penaltyScore1, penaltyScore2) trong thể thức KNOCKOUT phải phân định thắng thua");
            }
        }

        String winner;
        if (s1 > s2) {
            winner = participant1;
        } else if (s2 > s1) {
            winner = participant2;
        } else if (pen1 != null && pen2 != null && !Objects.equals(pen1, pen2)) {
            winner = pen1 > pen2 ? participant1 : participant2;
        } else {
            winner = "DRAW";
        }

        ResultPersistenceService.SaveOutcome outcome = persistenceService.saveRecordedResultAndCheckRecalc(
                request.getMatchId(),
                s1,
                s2,
                pen1,
                pen2,
                winner,
                request.getNotes(),
                competitionId,
                request.getDetails()
        );
        if (outcome.rankingSyncTaskId() != null) {
            executeRankingSyncTask(outcome.rankingSyncTaskId(), callerBearerToken);
        }
        return outcome.response();
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

                // Step 2: Recalculate standings in Ranking Service (skip point table for KNOCKOUT)
                boolean isKnockout = isKnockoutCompetition(competitionId);
                if (isKnockout) {
                    persistenceService.markCompletedWithoutRankingIfVersion(resultId, jobVersion, claimId, competitionId);
                } else {
                    if (!initial.isRankingSynced()) {
                        matchAndRankingClient.recalculateRanking(competitionId, bearerToken);
                    }
                    persistenceService.markAllSyncedIfVersion(resultId, jobVersion, claimId, competitionId);
                }
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
        return patchNotesOnly(id, updates, null);
    }

    public Result patchNotesOnly(Long id, Map<String, Object> updates, String callerBearerToken) {
        ResultPersistenceService.PatchOutcome outcome = persistenceService.patchNotesOnlyWithRecalc(id, updates);
        if (outcome.rankingSyncTaskId() != null) {
            executeRankingSyncTask(outcome.rankingSyncTaskId(), callerBearerToken);
        }
        return outcome.result();
    }

    public void deleteResult(Long id) {
        deleteResult(id, null);
    }

    public void deleteResult(Long id, String callerBearerToken) {
        Long taskId = persistenceService.deleteResultByIdWithRecalc(id);
        if (taskId != null) {
            executeRankingSyncTask(taskId, callerBearerToken);
        }
    }

    public void executeRankingSyncTask(Long taskId, String bearerToken) {
        if (rankingSyncTaskRepository == null || taskId == null) {
            return;
        }
        Optional<RankingSyncTask> taskOpt = rankingSyncTaskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return;
        }
        RankingSyncTask task = taskOpt.get();
        if ("SYNCED".equalsIgnoreCase(task.getStatus()) || task.isPermanentFailure()) {
            return;
        }
        try {
            if (isKnockoutCompetition(task.getCompetitionId())) {
                persistenceService.markRankingTaskSynced(taskId);
                return;
            }
            matchAndRankingClient.recalculateRanking(task.getCompetitionId(), bearerToken);
            persistenceService.markRankingTaskSynced(taskId);
        } catch (PermanentSyncException ex) {
            persistenceService.markRankingTaskFailed(taskId, ex.getMessage(), true, null);
        } catch (TransientSyncException ex) {
            int nextAttempt = task.getAttempts() + 1;
            long backoffSeconds = Math.min(300L, 15L * nextAttempt);
            persistenceService.markRankingTaskFailed(taskId, ex.getMessage(), false,
                    LocalDateTime.now().plusSeconds(backoffSeconds));
        } catch (ResponseStatusException ex) {
            boolean perm = ex.getStatusCode().is4xxClientError()
                    && ex.getStatusCode().value() != 408
                    && ex.getStatusCode().value() != 429;
            int nextAttempt = task.getAttempts() + 1;
            LocalDateTime nextRetry = perm ? null : LocalDateTime.now().plusSeconds(Math.min(300L, 15L * nextAttempt));
            persistenceService.markRankingTaskFailed(taskId,
                    ex.getReason() != null ? ex.getReason() : ex.getMessage(), perm, nextRetry);
        } catch (Exception ex) {
            int nextAttempt = task.getAttempts() + 1;
            persistenceService.markRankingTaskFailed(taskId, ex.getMessage(), false,
                    LocalDateTime.now().plusSeconds(Math.min(300L, 15L * nextAttempt)));
        }
    }

    private void assertMatchEligibleForResult(Map<String, Object> match, Long matchId) {
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy thông tin trận đấu #" + matchId);
        }
        String mStatus = match.get("status") != null ? String.valueOf(match.get("status")).trim().toUpperCase() : "";
        if (!"ONGOING".equals(mStatus) && !"FINISHED".equals(mStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chỉ được ghi kết quả khi trận đấu đang diễn ra (ONGOING) hoặc đã kết thúc (FINISHED); trạng thái hiện tại: " + mStatus);
        }
        LocalDateTime matchTime = parseLocalDateTime(match.get("matchTime"));
        if (matchTime != null && matchTime.isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Trận đấu #" + matchId + " chưa tới giờ thi đấu (" + matchTime + "), chưa thể ghi kết quả");
        }
    }

    private boolean isKnockoutCompetition(Long competitionId) {
        if (competitionId == null) {
            return false;
        }
        Map<String, Object> fmtMap = matchAndRankingClient.getCompetitionFormat(competitionId);
        if (fmtMap == null || fmtMap.get("formatType") == null) {
            return false;
        }
        String fmt = String.valueOf(fmtMap.get("formatType")).trim().toUpperCase();
        return "KNOCKOUT".equals(fmt) || "SINGLE_ELIMINATION".equals(fmt);
    }

    private LocalDateTime parseLocalDateTime(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDateTime ldt) return ldt;
        if (val instanceof List<?> arr && arr.size() >= 5) {
            int sec = arr.size() >= 6 ? Integer.parseInt(String.valueOf(arr.get(5))) : 0;
            return LocalDateTime.of(
                    Integer.parseInt(String.valueOf(arr.get(0))),
                    Integer.parseInt(String.valueOf(arr.get(1))),
                    Integer.parseInt(String.valueOf(arr.get(2))),
                    Integer.parseInt(String.valueOf(arr.get(3))),
                    Integer.parseInt(String.valueOf(arr.get(4))),
                    sec
            );
        }
        String text = String.valueOf(val).trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) return null;
        try {
            return LocalDateTime.parse(text);
        } catch (Exception ex) {
            return null;
        }
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
