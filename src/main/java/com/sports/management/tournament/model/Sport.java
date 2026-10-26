package com.sports.management.tournament.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "sports")
public class Sport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Sport name is required")
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank(message = "Sport type is required")
    private String sportType; // TEAM, INDIVIDUAL, DUAL

    private String description;

    public Sport() {
    }

    public Sport(Long id, String name, String sportType, String description) {
        this.id = id;
        this.name = name;
        this.sportType = sportType;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSportType() {
        return sportType;
    }

    public void setSportType(String sportType) {
        this.sportType = sportType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
