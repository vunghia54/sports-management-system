package com.sports.management.result.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "results")
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Match ID is required")
    @Column(nullable = false, unique = true)
    private Long matchId;

    private Integer scoreParticipant1 = 0;

    private Integer scoreParticipant2 = 0;

    private String winner; // participant1, participant2, or DRAW

    private String status = "PENDING_CONFIRMATION"; // PENDING_CONFIRMATION, CONFIRMED

    private String confirmedBy;

    private LocalDateTime confirmedAt;

    private String notes;

    private Long competitionId;

    private String syncStatus = "NOT_REQUIRED"; // NOT_REQUIRED, PENDING, SYNCED, FAILED

    private Boolean matchSynced = Boolean.FALSE;

    private Boolean rankingSynced = Boolean.FALSE;

    private Integer syncAttempts = 0;

    private LocalDateTime lastSyncAttemptAt;

    private LocalDateTime nextRetryAt;

    @Column(length = 1000)
    private String syncError;

    private Boolean syncPermanentFailure = Boolean.FALSE;

    private Long resultVersion = 1L;

    private String syncClaimId;

    private LocalDateTime syncLeaseUntil;

    public Result() {
    }

    public Result(Long id, Long matchId, Integer scoreParticipant1, Integer scoreParticipant2, String winner, String status, String confirmedBy, LocalDateTime confirmedAt, String notes) {
        this.id = id;
        this.matchId = matchId;
        this.scoreParticipant1 = scoreParticipant1;
        this.scoreParticipant2 = scoreParticipant2;
        this.winner = winner;
        this.status = status;
        this.confirmedBy = confirmedBy;
        this.confirmedAt = confirmedAt;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public Integer getScoreParticipant1() {
        return scoreParticipant1;
    }

    public void setScoreParticipant1(Integer scoreParticipant1) {
        this.scoreParticipant1 = scoreParticipant1;
    }

    public Integer getScoreParticipant2() {
        return scoreParticipant2;
    }

    public void setScoreParticipant2(Integer scoreParticipant2) {
        this.scoreParticipant2 = scoreParticipant2;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(String confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(Long competitionId) {
        this.competitionId = competitionId;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public boolean isMatchSynced() {
        return Boolean.TRUE.equals(matchSynced);
    }

    public void setMatchSynced(Boolean matchSynced) {
        this.matchSynced = matchSynced != null ? matchSynced : Boolean.FALSE;
    }

    public boolean isRankingSynced() {
        return Boolean.TRUE.equals(rankingSynced);
    }

    public void setRankingSynced(Boolean rankingSynced) {
        this.rankingSynced = rankingSynced != null ? rankingSynced : Boolean.FALSE;
    }

    public int getSyncAttempts() {
        return syncAttempts != null ? syncAttempts : 0;
    }

    public void setSyncAttempts(Integer syncAttempts) {
        this.syncAttempts = syncAttempts != null ? syncAttempts : 0;
    }

    public LocalDateTime getLastSyncAttemptAt() {
        return lastSyncAttemptAt;
    }

    public void setLastSyncAttemptAt(LocalDateTime lastSyncAttemptAt) {
        this.lastSyncAttemptAt = lastSyncAttemptAt;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public String getSyncError() {
        return syncError;
    }

    public void setSyncError(String syncError) {
        this.syncError = syncError;
    }

    public boolean isSyncPermanentFailure() {
        return Boolean.TRUE.equals(syncPermanentFailure);
    }

    public void setSyncPermanentFailure(Boolean syncPermanentFailure) {
        this.syncPermanentFailure = syncPermanentFailure != null ? syncPermanentFailure : Boolean.FALSE;
    }

    public long getResultVersion() {
        return resultVersion != null ? resultVersion : 1L;
    }

    public void setResultVersion(Long resultVersion) {
        this.resultVersion = resultVersion != null ? resultVersion : 1L;
    }

    public String getSyncClaimId() {
        return syncClaimId;
    }

    public void setSyncClaimId(String syncClaimId) {
        this.syncClaimId = syncClaimId;
    }

    public LocalDateTime getSyncLeaseUntil() {
        return syncLeaseUntil;
    }

    public void setSyncLeaseUntil(LocalDateTime syncLeaseUntil) {
        this.syncLeaseUntil = syncLeaseUntil;
    }
}
