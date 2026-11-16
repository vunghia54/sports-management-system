package com.sports.management.team.controller;

import com.sports.management.team.client.AthleteClient;
import com.sports.management.team.model.Team;
import com.sports.management.team.model.TeamMember;
import com.sports.management.team.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService teamService;
    private final AthleteClient athleteClient;

    @Value("${spring.application.name:team-service}")
    private String applicationName;

    @Value("${server.port:8085}")
    private int serverPort;

    public TeamController(TeamService teamService, AthleteClient athleteClient) {
        this.teamService = teamService;
        this.athleteClient = athleteClient;
    }

    // TH1: Hello, Info, Statistics
    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> hello() {
        return ResponseEntity.ok(Map.of(
                "message", "Welcome to Team Service",
                "instanceId", applicationName + ":" + serverPort
        ));
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
                "service", "Team Service",
                "serviceId", applicationName.toUpperCase(),
                "instanceId", applicationName + ":" + serverPort,
                "version", "1.0",
                "status", "UP"
        ));
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> statistics() {
        return ResponseEntity.ok(teamService.getStatistics());
    }

    // TH7: Service Discovery Verification (TEAM-SERVICE -> ATHLETE-SERVICE)
    @GetMapping("/discovery/athlete-instance")
    public ResponseEntity<Map<String, Object>> discoverAthleteInstance() {
        return ResponseEntity.ok(athleteClient.discoverAthleteInstance());
    }

    // TH3: Paged Search / Filter / Sort
    @GetMapping("/paged")
    public ResponseEntity<TeamService.TeamPage> getTeamsPaged(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String shortName,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return ResponseEntity.ok(teamService.searchTeamsPaged(name, shortName, keyword, page, size, sortBy, direction));
    }

    @GetMapping
    public ResponseEntity<List<Team>> getAllTeams(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String shortName,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return ResponseEntity.ok(teamService.filterTeams(name, shortName, keyword, sortBy, direction));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Team> getTeamById(@PathVariable Long id) {
        Team team = teamService.getTeamById(id);
        if (team == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found with id: " + id);
        }
        String instanceId = applicationName + ":" + serverPort;
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .header("X-Instance-Port", String.valueOf(serverPort))
                .body(team);
    }

    @PostMapping
    public ResponseEntity<Team> createTeam(
            @Valid @RequestBody Team team,
            Authentication authentication) {
        if (authentication != null && authentication.getName() != null
                && (team.getOwnerUsername() == null || team.getOwnerUsername().isBlank())) {
            team.setOwnerUsername(authentication.getName());
        }
        Team created = teamService.createTeam(team);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Instance-Id", applicationName + ":" + serverPort)
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Team> updateTeam(
            @PathVariable Long id,
            @Valid @RequestBody Team team,
            Authentication authentication) {
        Team existing = teamService.getTeamById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found with id: " + id);
        }
        checkOwnership(existing, authentication);
        Team updated = teamService.updateTeam(id, team);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Team> patchTeam(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates,
            Authentication authentication) {
        Team existing = teamService.getTeamById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found with id: " + id);
        }
        checkOwnership(existing, authentication);
        Team updated = teamService.patchTeam(id, updates);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable Long id,
            Authentication authentication) {
        Team existing = teamService.getTeamById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found with id: " + id);
        }
        checkOwnership(existing, authentication);
        List<TeamMember> members = teamService.getMembersByTeam(id);
        if (members != null && !members.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không thể xóa đội tuyển '" + existing.getName() + "' vì đang có " + members.size() + " thành viên trong đội hình. Vui lòng xóa thành viên khỏi đội trước."
            );
        }
        teamService.deleteTeam(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<TeamMember> addMember(
            @PathVariable Long id,
            @RequestBody TeamMember member,
            Authentication authentication) {
        Team existing = teamService.getTeamById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found with id: " + id);
        }
        checkOwnership(existing, authentication);
        member.setTeamId(id);
        TeamMember created = teamService.addMember(member);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Athlete-Provider-Instance", athleteClient.getLastHandledInstance())
                .body(created);
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<TeamMember>> getMembers(@PathVariable Long id) {
        if (teamService.getTeamById(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found with id: " + id);
        }
        return ResponseEntity.ok(teamService.getMembersByTeam(id));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long memberId,
            Authentication authentication) {
        Team existing = teamService.getTeamById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found with id: " + id);
        }
        checkOwnership(existing, authentication);
        if (!teamService.removeMember(memberId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team member not found with id: " + memberId);
        }
        return ResponseEntity.noContent().build();
    }

    private void checkOwnership(Team team, Authentication authentication) {
        if (authentication == null) return;
        boolean privileged = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_ORGANIZER"));
        if (privileged) return;
        if (team.getOwnerUsername() != null && !team.getOwnerUsername().equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not own this team");
        }
    }
}
