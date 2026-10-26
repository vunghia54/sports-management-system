package com.sports.management.notification.messaging;

import java.io.Serializable;

public class MatchFinishedEvent implements Serializable {

    private String event;
    private Long matchId;
    private Long tournamentId;
    private String participant1;
    private String participant2;

    public MatchFinishedEvent() {
    }

    public MatchFinishedEvent(String event, Long matchId, Long tournamentId, String participant1, String participant2) {
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
}
