package com.sports.management.tournament.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "competitions")
public class Competition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Tournament ID is required")
    @Column(nullable = false)
    private Long tournamentId;

    @NotNull(message = "Sport ID is required")
    @Column(nullable = false)
    private Long sportId;

    @NotBlank(message = "Competition name is required")
    @Column(nullable = false)
    private String name;

    private String status = "OPEN_REGISTRATION"; // DRAFT, OPEN_REGISTRATION, SCHEDULED, ONGOING, FINISHED

    public Competition() {
    }

    public Competition(Long id, Long tournamentId, Long sportId, String name, String status) {
        this.id = id;
        this.tournamentId = tournamentId;
        this.sportId = sportId;
        this.name = name;
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

    public Long getSportId() {
        return sportId;
    }

    public void setSportId(Long sportId) {
        this.sportId = sportId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompetitionName() {
        return name;
    }

    public void setCompetitionName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
