package com.sports.management.match.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Tournament ID is required")
    @Column(name = "tournament_id", nullable = false)
    private Long tournamentId;

    @NotNull(message = "Competition ID is required")
    @Column(name = "competition_id")
    private Long competitionId;

    private String round;

    private String groupName;

    @NotNull(message = "Match time is required")
    @JsonAlias({"scheduledTime", "scheduled_time"})
    private LocalDateTime matchTime;

    private String location;

    private String referee;

    @Column(name = "referee_username")
    private String refereeUsername;

    @NotNull(message = "Participant 1 ID is required")
    @Column(name = "participant1_id")
    private Long participant1Id;

    @NotNull(message = "Participant 2 ID is required")
    @Column(name = "participant2_id")
    private Long participant2Id;

    private String participant1;

    private String participant2;

    private String status;

    private String ownerUsername;

    @Transient
    private Integer scoreParticipant1;

    @Transient
    private Integer scoreParticipant2;

    @Transient
    private Integer penaltyScore1;

    @Transient
    private Integer penaltyScore2;

    public Match() {
    }

    public Match(
            Long id,
            Long tournamentId,
            String round,
            LocalDateTime matchTime,
            String location,
            String participant1,
            String participant2,
            String status) {

        this.id = id;
        this.tournamentId = tournamentId;
        this.round = round;
        this.matchTime = matchTime;
        this.location = location;
        this.participant1 = participant1;
        this.participant2 = participant2;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(Long tournamentId) {
        this.tournamentId = tournamentId;
    }

    public Long getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(Long competitionId) {
        this.competitionId = competitionId;
    }

    public String getRound() {
        return round;
    }

    public void setRound(String round) {
        this.round = round;
    }

    public LocalDateTime getMatchTime() {
        return matchTime;
    }

    public void setMatchTime(LocalDateTime matchTime) {
        this.matchTime = matchTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Long getParticipant1Id() {
        return participant1Id;
    }

    public void setParticipant1Id(Long participant1Id) {
        this.participant1Id = participant1Id;
    }

    public Long getParticipant2Id() {
        return participant2Id;
    }

    public void setParticipant2Id(Long participant2Id) {
        this.participant2Id = participant2Id;
    }

    public String getParticipant1() {
        return participant1;
    }

    public void setParticipant1(String participant1) {
        this.participant1 = participant1;
    }

    public String getParticipant2() {
        return participant2;
    }

    public void setParticipant2(String participant2) {
        this.participant2 = participant2;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getReferee() {
        return referee;
    }

    public void setReferee(String referee) {
        this.referee = referee;
    }

    public String getRefereeUsername() {
        return refereeUsername;
    }

    public void setRefereeUsername(String refereeUsername) {
        this.refereeUsername = refereeUsername;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
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

    public Integer getPenaltyScore1() {
        return penaltyScore1;
    }

    public void setPenaltyScore1(Integer penaltyScore1) {
        this.penaltyScore1 = penaltyScore1;
    }

    public Integer getPenaltyScore2() {
        return penaltyScore2;
    }

    public void setPenaltyScore2(Integer penaltyScore2) {
        this.penaltyScore2 = penaltyScore2;
    }
}
