package com.sports.management.match.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "registrations")
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Competition ID is required")
    @Column(nullable = false)
    private Long competitionId;

    @NotNull(message = "Participant type is required")
    private String participantType; // TEAM, ATHLETE

    @NotNull(message = "Participant reference ID is required")
    private Long participantReferenceId; // teamId or athleteId

    private String participantName;

    private LocalDateTime registrationDate = LocalDateTime.now();

    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, WITHDRAWN

    private String rejectionReason;

    public Registration() {
    }

    public Registration(Long id, Long competitionId, String participantType, Long participantReferenceId, String participantName, LocalDateTime registrationDate, String status, String rejectionReason) {
        this.id = id;
        this.competitionId = competitionId;
        this.participantType = participantType;
        this.participantReferenceId = participantReferenceId;
        this.participantName = participantName;
        this.registrationDate = registrationDate;
        this.status = status;
        this.rejectionReason = rejectionReason;
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

    public String getParticipantType() {
        return participantType;
    }

    public void setParticipantType(String participantType) {
        this.participantType = participantType;
    }

    public Long getParticipantReferenceId() {
        return participantReferenceId;
    }

    public void setParticipantReferenceId(Long participantReferenceId) {
        this.participantReferenceId = participantReferenceId;
    }

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
