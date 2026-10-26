package com.sports.management.result.model;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public class RecordResultRequest {

    @NotNull(message = "Match ID is required")
    private Long matchId;

    private Integer scoreParticipant1 = 0;

    private Integer scoreParticipant2 = 0;

    private String notes;

    private List<ResultDetail> details;

    public RecordResultRequest() {
    }

    public RecordResultRequest(Long matchId, Integer scoreParticipant1, Integer scoreParticipant2, String notes, List<ResultDetail> details) {
        this.matchId = matchId;
        this.scoreParticipant1 = scoreParticipant1;
        this.scoreParticipant2 = scoreParticipant2;
        this.notes = notes;
        this.details = details;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<ResultDetail> getDetails() {
        return details;
    }

    public void setDetails(List<ResultDetail> details) {
        this.details = details;
    }
}
