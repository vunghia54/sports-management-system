package com.sports.management.result.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ranking_sync_tasks")
public class RankingSyncTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long competitionId;

    @Column(nullable = false)
    private String reason; // RESULT_REOPENED, RESULT_EDITED, RESULT_DELETED

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, SYNCED, FAILED

    private Integer attempts = 0;

    private LocalDateTime nextRetryAt;

    private LocalDateTime lastAttemptAt;

    @Column(length = 1000)
    private String lastError;

    private Boolean permanentFailure = Boolean.FALSE;

    private LocalDateTime createdAt = LocalDateTime.now();

    public RankingSyncTask() {
    }

    public RankingSyncTask(Long competitionId, String reason) {
        this.competitionId = competitionId;
        this.reason = reason;
        this.status = "PENDING";
        this.attempts = 0;
        this.nextRetryAt = LocalDateTime.now();
        this.permanentFailure = Boolean.FALSE;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(Long competitionId) {
        this.competitionId = competitionId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts != null ? attempts : 0;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts != null ? attempts : 0;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public LocalDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(LocalDateTime lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public boolean isPermanentFailure() {
        return Boolean.TRUE.equals(permanentFailure);
    }

    public void setPermanentFailure(Boolean permanentFailure) {
        this.permanentFailure = permanentFailure != null ? permanentFailure : Boolean.FALSE;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
