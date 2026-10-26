package com.sports.management.ranking.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "ranking_items")
public class RankingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Ranking ID is required")
    @Column(nullable = false)
    private Long rankingId;

    private Integer rankPosition = 1;

    @NotBlank(message = "Participant name is required")
    @Column(nullable = false)
    private String participantName;

    private Integer matchesPlayed = 0;

    private Integer won = 0;

    private Integer drawn = 0;

    private Integer lost = 0;

    private Integer points = 0;

    private Integer goalsFor = 0;

    private Integer goalsAgainst = 0;

    private Integer goalDifference = 0;

    public RankingItem() {
    }

    public RankingItem(Long id, Long rankingId, Integer rankPosition, String participantName, Integer matchesPlayed, Integer won, Integer drawn, Integer lost, Integer points, Integer goalsFor, Integer goalsAgainst, Integer goalDifference) {
        this.id = id;
        this.rankingId = rankingId;
        this.rankPosition = rankPosition;
        this.participantName = participantName;
        this.matchesPlayed = matchesPlayed;
        this.won = won;
        this.drawn = drawn;
        this.lost = lost;
        this.points = points;
        this.goalsFor = goalsFor;
        this.goalsAgainst = goalsAgainst;
        this.goalDifference = goalDifference;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRankingId() {
        return rankingId;
    }

    public void setRankingId(Long rankingId) {
        this.rankingId = rankingId;
    }

    public Integer getRankPosition() {
        return rankPosition;
    }

    public void setRankPosition(Integer rankPosition) {
        this.rankPosition = rankPosition;
    }

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public Integer getMatchesPlayed() {
        return matchesPlayed;
    }

    public void setMatchesPlayed(Integer matchesPlayed) {
        this.matchesPlayed = matchesPlayed;
    }

    public Integer getWon() {
        return won;
    }

    public void setWon(Integer won) {
        this.won = won;
    }

    public Integer getDrawn() {
        return drawn;
    }

    public void setDrawn(Integer drawn) {
        this.drawn = drawn;
    }

    public Integer getLost() {
        return lost;
    }

    public void setLost(Integer lost) {
        this.lost = lost;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Integer getGoalsFor() {
        return goalsFor;
    }

    public void setGoalsFor(Integer goalsFor) {
        this.goalsFor = goalsFor;
    }

    public Integer getGoalsAgainst() {
        return goalsAgainst;
    }

    public void setGoalsAgainst(Integer goalsAgainst) {
        this.goalsAgainst = goalsAgainst;
    }

    public Integer getGoalDifference() {
        return goalDifference;
    }

    public void setGoalDifference(Integer goalDifference) {
        this.goalDifference = goalDifference;
    }
}
