package com.sports.management.team.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "team_members")
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long teamId;

    @Column(nullable = false)
    private Long athleteId;

    private Integer jerseyNumber;
    private String position;
    private LocalDate joinDate = LocalDate.now();
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, INJURED

    public TeamMember() {
    }

    public TeamMember(Long id, Long teamId, Long athleteId, Integer jerseyNumber, String position, LocalDate joinDate, String status) {
        this.id = id;
        this.teamId = teamId;
        this.athleteId = athleteId;
        this.jerseyNumber = jerseyNumber;
        this.position = position;
        this.joinDate = joinDate;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Long getAthleteId() {
        return athleteId;
    }

    public void setAthleteId(Long athleteId) {
        this.athleteId = athleteId;
    }

    public Integer getJerseyNumber() {
        return jerseyNumber;
    }

    public void setJerseyNumber(Integer jerseyNumber) {
        this.jerseyNumber = jerseyNumber;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDate joinDate) {
        this.joinDate = joinDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
