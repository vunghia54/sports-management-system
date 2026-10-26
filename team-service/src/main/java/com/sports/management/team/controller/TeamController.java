package com.sports.management.team.controller;

import com.sports.management.team.model.Team;
import com.sports.management.team.model.TeamMember;
import com.sports.management.team.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public ResponseEntity<List<Team>> getAllTeams() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Team> getTeamById(@PathVariable Long id) {
        Team team = teamService.getTeamById(id);
        if (team == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found with id: " + id);
        }
        return ResponseEntity.ok(team);
    }

    @PostMapping
    public ResponseEntity<Team> createTeam(@Valid @RequestBody Team team) {
        Team created = teamService.createTeam(team);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<TeamMember> addMember(
            @PathVariable Long id,
            @RequestBody TeamMember member) {
        member.setTeamId(id);
        TeamMember created = teamService.addMember(member);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<TeamMember>> getMembers(@PathVariable Long id) {
        return ResponseEntity.ok(teamService.getMembersByTeam(id));
    }
}
