package com.sports.management.match.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "participants")
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Competition ID is required")
    @Column(nullable = false)
    private Long competitionId;

    private Long registrationId;

    @NotBlank(message = "Participant type is required")
    private String participantType; // TEAM, ATHLETE

    @NotNull(message = "Reference ID is required")
    private Long referenceId; // teamId or athleteId

    @NotBlank(message = "Display name is required")
    @Column(nullable = false)
    private String displayName;

    private Integer seedNumber;

    public Participant() {
    }

    public Participant(Long id, Long competitionId, Long registrationId, String participantType, Long referenceId, String displayName, Integer seedNumber) {
        this.id = id;
        this.competitionId = competitionId;
        this.registrationId = registrationId;
        this.participantType = participantType;
        this.referenceId = referenceId;
        this.displayName = displayName;
        this.seedNumber = seedNumber;
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

    public Long getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Long registrationId) {
        this.registrationId = registrationId;
    }

    public String getParticipantType() {
        return participantType;
    }

    public void setParticipantType(String participantType) {
        this.participantType = participantType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getSeedNumber() {
        return seedNumber;
    }

    public void setSeedNumber(Integer seedNumber) {
        this.seedNumber = seedNumber;
    }
}
