package com.sports.management.tournament.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "competition_rules")
public class CompetitionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long competitionId;

    private Integer maxParticipants = 16;
    private Integer minTeamMembers = 5;
    private Integer maxTeamMembers = 20;
    private LocalDateTime registrationDeadline;
    private String scoringMethod = "STANDARD_POINTS"; // STANDARD_POINTS, TIME_BASED, SCORE_BASED
    private String eligibilityCriteria;

    public CompetitionRule() {
    }

    public CompetitionRule(Long id, Long competitionId, Integer maxParticipants, Integer minTeamMembers, Integer maxTeamMembers, LocalDateTime registrationDeadline, String scoringMethod, String eligibilityCriteria) {
        this.id = id;
        this.competitionId = competitionId;
        this.maxParticipants = maxParticipants;
        this.minTeamMembers = minTeamMembers;
        this.maxTeamMembers = maxTeamMembers;
        this.registrationDeadline = registrationDeadline;
        this.scoringMethod = scoringMethod;
        this.eligibilityCriteria = eligibilityCriteria;
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

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public Integer getMinTeamMembers() {
        return minTeamMembers;
    }

    public void setMinTeamMembers(Integer minTeamMembers) {
        this.minTeamMembers = minTeamMembers;
    }

    public Integer getMaxTeamMembers() {
        return maxTeamMembers;
    }

    public void setMaxTeamMembers(Integer maxTeamMembers) {
        this.maxTeamMembers = maxTeamMembers;
    }

    public LocalDateTime getRegistrationDeadline() {
        return registrationDeadline;
    }

    public void setRegistrationDeadline(LocalDateTime registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public String getScoringMethod() {
        return scoringMethod;
    }

    public void setScoringMethod(String scoringMethod) {
        this.scoringMethod = scoringMethod;
    }

    public String getEligibilityCriteria() {
        return eligibilityCriteria;
    }

    public void setEligibilityCriteria(String eligibilityCriteria) {
        this.eligibilityCriteria = eligibilityCriteria;
    }
}
