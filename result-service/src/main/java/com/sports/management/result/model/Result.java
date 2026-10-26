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
}
