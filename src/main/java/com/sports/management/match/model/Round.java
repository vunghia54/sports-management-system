package com.sports.management.match.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "rounds")
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Competition ID is required")
    @Column(nullable = false)
    private Long competitionId;

    @NotBlank(message = "Round name is required")
    @Column(nullable = false)
    private String roundName; // Vòng bảng, Tứ kết, Bán kết, Chung kết

    private Integer roundOrder = 1;

    private String status = "SCHEDULED"; // SCHEDULED, IN_PROGRESS, COMPLETED

    public Round() {
    }

    public Round(Long id, Long competitionId, String roundName, Integer roundOrder, String status) {
        this.id = id;
        this.competitionId = competitionId;
        this.roundName = roundName;
        this.roundOrder = roundOrder;
        this.status = status;
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

    public String getRoundName() {
        return roundName;
    }

    public void setRoundName(String roundName) {
        this.roundName = roundName;
    }

    public Integer getRoundOrder() {
        return roundOrder;
    }

    public void setRoundOrder(Integer roundOrder) {
        this.roundOrder = roundOrder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
