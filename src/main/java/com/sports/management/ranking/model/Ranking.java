package com.sports.management.ranking.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "rankings")
public class Ranking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Competition ID is required")
    @Column(nullable = false, unique = true)
    private Long competitionId;

    private String rankingName;

    private LocalDateTime updatedAt = LocalDateTime.now();

    public Ranking() {
    }

    public Ranking(Long id, Long competitionId, String rankingName, LocalDateTime updatedAt) {
        this.id = id;
        this.competitionId = competitionId;
        this.rankingName = rankingName;
        this.updatedAt = updatedAt;
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

    public String getRankingName() {
        return rankingName;
    }

    public void setRankingName(String rankingName) {
        this.rankingName = rankingName;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
