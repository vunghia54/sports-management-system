package com.sports.management.tournament.model;

import jakarta.persistence.*;

@Entity
@Table(name = "competition_formats")
public class CompetitionFormat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long competitionId;

    private String formatType = "KNOCKOUT"; // ROUND_ROBIN, KNOCKOUT, GROUP_STAGE, HYBRID
    private Integer numberOfGroups = 1;
    private Integer qualifyingSpotsPerGroup = 2;
    private Boolean hasThirdPlaceMatch = false;

    public CompetitionFormat() {
    }

    public CompetitionFormat(Long id, Long competitionId, String formatType, Integer numberOfGroups, Integer qualifyingSpotsPerGroup, Boolean hasThirdPlaceMatch) {
        this.id = id;
        this.competitionId = competitionId;
        this.formatType = formatType;
        this.numberOfGroups = numberOfGroups;
        this.qualifyingSpotsPerGroup = qualifyingSpotsPerGroup;
        this.hasThirdPlaceMatch = hasThirdPlaceMatch;
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

    public String getFormatType() {
        return formatType;
    }

    public void setFormatType(String formatType) {
        this.formatType = formatType;
    }

    public Integer getNumberOfGroups() {
        return numberOfGroups;
    }

    public void setNumberOfGroups(Integer numberOfGroups) {
        this.numberOfGroups = numberOfGroups;
    }

    public Integer getQualifyingSpotsPerGroup() {
        return qualifyingSpotsPerGroup;
    }

    public void setQualifyingSpotsPerGroup(Integer qualifyingSpotsPerGroup) {
        this.qualifyingSpotsPerGroup = qualifyingSpotsPerGroup;
    }

    public Boolean getHasThirdPlaceMatch() {
        return hasThirdPlaceMatch;
    }

    public void setHasThirdPlaceMatch(Boolean hasThirdPlaceMatch) {
        this.hasThirdPlaceMatch = hasThirdPlaceMatch;
    }
}
