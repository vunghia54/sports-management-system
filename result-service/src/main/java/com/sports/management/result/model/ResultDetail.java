package com.sports.management.result.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "result_details")
public class ResultDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Result ID is required")
    @Column(nullable = false)
    private Long resultId;

    @NotBlank(message = "Detail type is required")
    private String detailType; // GOAL, SET_SCORE, CARD, POINT, FOUL

    private String participantName;

    private String athleteName;

    private String detailTime; // e.g., "45'+2", "Set 1 (21-18)"

    private String detailValue; // e.g., "1-0", "Yellow Card"

    public ResultDetail() {
    }

    public ResultDetail(Long id, Long resultId, String detailType, String participantName, String athleteName, String detailTime, String detailValue) {
        this.id = id;
        this.resultId = resultId;
        this.detailType = detailType;
        this.participantName = participantName;
        this.athleteName = athleteName;
        this.detailTime = detailTime;
        this.detailValue = detailValue;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResultId() {
        return resultId;
    }

    public void setResultId(Long resultId) {
        this.resultId = resultId;
    }

    public String getDetailType() {
        return detailType;
    }

    public void setDetailType(String detailType) {
        this.detailType = detailType;
    }

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public String getAthleteName() {
        return athleteName;
    }

    public void setAthleteName(String athleteName) {
        this.athleteName = athleteName;
    }

    public String getDetailTime() {
        return detailTime;
    }

    public void setDetailTime(String detailTime) {
        this.detailTime = detailTime;
    }

    public String getDetailValue() {
        return detailValue;
    }

    public void setDetailValue(String detailValue) {
        this.detailValue = detailValue;
    }
}
