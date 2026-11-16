package com.sports.management.messaging.event;

public class MatchFinishedEvent {

    private String event;
    private Long matchId;
    private Long tournamentId;
    private String participant1;
    private String participant2;
    private Integer scoreParticipant1;
    private Integer scoreParticipant2;
    private Integer penaltyScore1;
    private Integer penaltyScore2;

    public MatchFinishedEvent() {
    }

    public MatchFinishedEvent(
            String event,
            Long matchId,
            Long tournamentId,
            String participant1,
            String participant2) {

        this.event = event;
        this.matchId = matchId;
        this.tournamentId = tournamentId;
        this.participant1 = participant1;
        this.participant2 = participant2;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public Long getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(Long tournamentId) {
        this.tournamentId = tournamentId;
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