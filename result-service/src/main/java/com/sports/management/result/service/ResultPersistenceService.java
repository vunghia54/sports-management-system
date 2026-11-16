package com.sports.management.result.service;

import com.sports.management.exception.ResourceNotFoundException;
import com.sports.management.result.model.Result;
import com.sports.management.result.model.ResultDetail;
import com.sports.management.result.model.ResultResponse;
import com.sports.management.result.repository.ResultDetailRepository;
import com.sports.management.result.repository.ResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ResultPersistenceService {

    private final ResultRepository resultRepository;
    private final ResultDetailRepository resultDetailRepository;

    public ResultPersistenceService(
            ResultRepository resultRepository,
            ResultDetailRepository resultDetailRepository) {
        this.resultRepository = resultRepository;
        this.resultDetailRepository = resultDetailRepository;
    }

    @Transactional
    public ResultResponse saveRecordedResult(
            Long matchId,
            Integer score1,
            Integer score2,
            String winner,
            String notes,
            Long competitionId,
            List<ResultDetail> details) {

        Result result = resultRepository.findByMatchId(matchId).orElse(null);
        long nextVersion;
        if (result == null) {
            result = new Result();
            nextVersion = 1L;
        } else {
            nextVersion = result.getResultVersion() + 1L;
        }

        result.setMatchId(matchId);
        result.setScoreParticipant1(score1);
        result.setScoreParticipant2(score2);
        result.setWinner(winner);
        result.setNotes(notes);
        result.setCompetitionId(competitionId);
        result.setResultVersion(nextVersion);

        // Every score edit invalidates previous confirmation/sync jobs cleanly
        result.setStatus("PENDING_CONFIRMATION");
        result.setConfirmedBy(null);
        result.setConfirmedAt(null);
        result.setSyncStatus("NOT_REQUIRED");
        result.setMatchSynced(false);
        result.setRankingSynced(false);
        result.setSyncAttempts(0);
        result.setLastSyncAttemptAt(null);
        result.setNextRetryAt(null);
        result.setSyncError(null);
        result.setSyncPermanentFailure(false);
        result.setSyncClaimId(null);
        result.setSyncLeaseUntil(null);

        Result savedResult = resultRepository.saveAndFlush(result);

        List<ResultDetail> savedDetails = Collections.emptyList();
        if (details != null && !details.isEmpty()) {
            resultDetailRepository.deleteByResultId(savedResult.getId());
            for (ResultDetail detail : details) {
                detail.setResultId(savedResult.getId());
            }
            savedDetails = resultDetailRepository.saveAll(details);
        }

        return new ResultResponse(savedResult, savedDetails);
    }

    @Transactional
    public ResultResponse confirmResultInDb(Long id, String confirmedBy, Long competitionId) {
        Result result = resultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found with id: " + id));

        boolean alreadyConfirmedAndSynced = "CONFIRMED".equalsIgnoreCase(result.getStatus())
                && "SYNCED".equalsIgnoreCase(result.getSyncStatus());
        if (alreadyConfirmedAndSynced) {
            List<ResultDetail> details = resultDetailRepository.findByResultId(result.getId());
            return new ResultResponse(result, details);
        }

        boolean wasConfirmed = "CONFIRMED".equalsIgnoreCase(result.getStatus());
        if (!wasConfirmed) {
            // Transitioning from PENDING_CONFIRMATION -> CONFIRMED starts a fresh syncable version
            result.setResultVersion(result.getResultVersion() + 1L);
            result.setMatchSynced(false);
            result.setRankingSynced(false);
            result.setSyncAttempts(0);
        }

        result.setStatus("CONFIRMED");
        result.setConfirmedBy(confirmedBy);
        result.setConfirmedAt(LocalDateTime.now());
        if (competitionId != null) {
            result.setCompetitionId(competitionId);
        }
        result.setSyncStatus("PENDING");
        result.setSyncError(null);
        result.setSyncPermanentFailure(false);
        result.setNextRetryAt(LocalDateTime.now());
        result.setSyncClaimId(null);
        result.setSyncLeaseUntil(null);

        Result savedResult = resultRepository.saveAndFlush(result);
        List<ResultDetail> details = resultDetailRepository.findByResultId(savedResult.getId());
        return new ResultResponse(savedResult, details);
    }

    @Transactional
    public boolean tryClaimSyncJob(Long resultId, long expectedVersion, String claimId, LocalDateTime leaseUntil) {
        return resultRepository.claimSyncJob(resultId, expectedVersion, claimId, leaseUntil, LocalDateTime.now()) > 0;
    }

    @Transactional
    public boolean updateCompetitionIdIfVersion(Long resultId, long expectedVersion, String claimId, Long competitionId) {
        if (competitionId == null) {
            return true;
        }
        return resultRepository.updateCompetitionIdIfVersion(resultId, expectedVersion, claimId, competitionId) > 0;
    }

    @Transactional
    public boolean markMatchSyncedIfVersion(Long resultId, long expectedVersion, String claimId, Long competitionId) {
        if (competitionId != null) {
            resultRepository.updateCompetitionIdIfVersion(resultId, expectedVersion, claimId, competitionId);
        }
        return resultRepository.markMatchSyncedIfVersion(resultId, expectedVersion, claimId, LocalDateTime.now()) > 0;
    }

    @Transactional
    public boolean markAllSyncedIfVersion(Long resultId, long expectedVersion, String claimId, Long competitionId) {
        if (competitionId != null) {
            resultRepository.updateCompetitionIdIfVersion(resultId, expectedVersion, claimId, competitionId);
        }
        return resultRepository.markAllSyncedIfVersion(resultId, expectedVersion, claimId, LocalDateTime.now()) > 0;
    }

    @Transactional
    public boolean markSyncFailedIfVersion(
            Long resultId,
            long expectedVersion,
            String claimId,
            String errorMessage,
            boolean permanentFailure,
            LocalDateTime nextRetryAt) {
        String safeError = errorMessage != null ? errorMessage : "";
        String trimmedError = safeError.length() > 950
                ? safeError.substring(0, 950)
                : safeError;
        return resultRepository.markSyncFailedIfVersion(
                resultId,
                expectedVersion,
                claimId,
                trimmedError,
                permanentFailure,
                permanentFailure ? null : nextRetryAt,
                LocalDateTime.now()
        ) > 0;
    }

    @Transactional
    public void releaseClaim(Long resultId, String claimId) {
        resultRepository.releaseSyncClaim(resultId, claimId);
    }

    @Transactional
    public Result patchNotesOnly(Long id, Map<String, Object> updates) {
        Result existing = resultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found with id: " + id));
        if (updates != null && updates.containsKey("notes")) {
            Object rawNotes = updates.get("notes");
            existing.setNotes(rawNotes != null ? String.valueOf(rawNotes) : null);
        }
        if (updates != null && updates.containsKey("status")) {
            Object rawStatus = updates.get("status");
            String nextStatus = rawStatus != null ? String.valueOf(rawStatus).trim().toUpperCase() : "";
            if ("PENDING_CONFIRMATION".equals(nextStatus) && !"PENDING_CONFIRMATION".equalsIgnoreCase(existing.getStatus())) {
                existing.setResultVersion(existing.getResultVersion() + 1L);
                existing.setStatus("PENDING_CONFIRMATION");
                existing.setConfirmedBy(null);
                existing.setConfirmedAt(null);
                existing.setSyncStatus("NOT_REQUIRED");
                existing.setMatchSynced(false);
                existing.setRankingSynced(false);
                existing.setSyncAttempts(0);
                existing.setSyncError(null);
                existing.setSyncPermanentFailure(false);
                existing.setNextRetryAt(null);
                existing.setSyncClaimId(null);
                existing.setSyncLeaseUntil(null);
            }
        }
        return resultRepository.saveAndFlush(existing);
    }

    @Transactional
    public void deleteResultById(Long id) {
        Result existing = resultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found with id: " + id));
        resultDetailRepository.deleteByResultId(existing.getId());
        resultRepository.delete(existing);
    }
}
