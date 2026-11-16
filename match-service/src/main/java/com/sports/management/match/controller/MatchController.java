package com.sports.management.match.controller;

import com.sports.management.match.client.TournamentClient;
import com.sports.management.match.model.Match;
import com.sports.management.match.service.MatchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {

    private final MatchService matchService;
    private final TournamentClient tournamentClient;
    private final String instanceId;

    public MatchController(
            MatchService matchService,
            TournamentClient tournamentClient,
            @Value("${eureka.instance.instance-id:${spring.application.name}:${server.port}}") String instanceId) {
        this.matchService = matchService;
        this.tournamentClient = tournamentClient;
        this.instanceId = instanceId;
    }

    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> sayHello() {
        return ResponseEntity.ok(Map.of(
                "service", "Match & Registration Service",
                "message", "Welcome to Match Service API (TH1 - TH7)",
                "instanceId", instanceId
        ));
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("serviceName", "MATCH-SERVICE");
        info.put("version", "v1");
        info.put("description", "Quản lý lịch thi đấu, đăng ký tham gia (Registration) và danh sách vận động viên/đội tham dự (Participant)");
        info.put("instanceId", instanceId);
        return ResponseEntity.ok(info);
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getMatchStatistics() {
        return ResponseEntity.ok(matchService.getMatchStatistics());
    }

    // SERVICE DISCOVERY & LOAD BALANCING CHECK (TH7)
    @GetMapping("/discovery/tournament-instance")
    public ResponseEntity<Map<String, Object>> discoverTournamentInstance() {
        return ResponseEntity.ok(tournamentClient.discoverTournamentInstance());
    }

    // GET ALL
    @GetMapping
    public ResponseEntity<List<Match>> getAllMatches() {
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(matchService.getAllMatches());
    }

    // GET PAGED & FILTERED (TH3)
    @GetMapping("/paged")
    public ResponseEntity<Page<Match>> searchMatches(
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) Long competitionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = "desc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(matchService.searchMatches(tournamentId, competitionId, status, keyword, date, pageable));
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Match> getMatchById(@PathVariable Long id) {
        Match match = matchService.getMatchById(id);

        if (match == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Match not found with id: " + id
            );
        }

        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(match);
    }

    // GET MATCHES BY TOURNAMENT
    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<Match>> getMatchesByTournament(
            @PathVariable Long tournamentId) {

        if (!matchService.tournamentExists(tournamentId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Tournament not found with id: " + tournamentId
            );
        }

        return ResponseEntity.ok(
                matchService.getMatchesByTournamentId(tournamentId)
        );
    }

    // GET MATCHES BY COMPETITION
    @GetMapping("/competition/{competitionId}")
    public ResponseEntity<List<Match>> getMatchesByCompetition(
            @PathVariable Long competitionId) {
        return ResponseEntity.ok(
                matchService.getMatchesByCompetitionId(competitionId)
        );
    }

    // CREATE
    @PostMapping
    public ResponseEntity<Match> createMatch(
            @RequestBody Match match,
            Authentication authentication) {

        assertAuthenticated(authentication);
        if (match.getTournamentId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tournament ID is required"
            );
        }

        if (!hasAuthority(authentication, "ROLE_ADMIN")) {
            if (!hasAuthority(authentication, "ROLE_ORGANIZER")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Only Organizer or Admin can create matches");
            }
            assertTournamentOwnership(match.getTournamentId(), authentication.getName());
        } else {
            assertTournamentExists(match.getTournamentId());
        }

        // Always derive ownership from authenticated identity, never from client input
        match.setOwnerUsername(authentication.getName());

        Match createdMatch = matchService.createMatch(match);

        if (createdMatch == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid tournament ID"
            );
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("X-Tournament-Provider-Instance", tournamentClient.getLastHandledInstance())
                .body(createdMatch);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Match> updateMatch(
            @PathVariable Long id,
            @RequestBody Match match,
            Authentication authentication) {

        assertAuthenticated(authentication);
        Match existing = matchService.getMatchById(id);
        if (existing == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Match not found with id: " + id
            );
        }

        if (match.getTournamentId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tournament ID is required"
            );
        }

        verifyMatchUpdatePermission(existing, match.getTournamentId(), authentication, false, match, null);

        Match updatedMatch = matchService.updateMatch(id, match);
        if (updatedMatch == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tournament ID");
        }
        return ResponseEntity.ok(updatedMatch);
    }

    // PATCH
    @PatchMapping("/{id}")
    public ResponseEntity<Match> patchMatch(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates,
            Authentication authentication) {

        assertAuthenticated(authentication);
        Match existing = matchService.getMatchById(id);
        if (existing == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Match not found with id: " + id
            );
        }

        Long targetTournamentId = existing.getTournamentId();
        if (updates != null && updates.containsKey("tournamentId") && updates.get("tournamentId") != null) {
            try {
                targetTournamentId = Long.valueOf(String.valueOf(updates.get("tournamentId")));
            } catch (NumberFormatException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tournament ID");
            }
        }

        verifyMatchUpdatePermission(existing, targetTournamentId, authentication, true, null, updates);

        Match updatedMatch = matchService.patchMatch(id, updates != null ? updates : Map.of());
        if (updatedMatch == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Match not found with id: " + id
            );
        }
        return ResponseEntity.ok(updatedMatch);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMatch(
            @PathVariable Long id,
            Authentication authentication) {

        assertAuthenticated(authentication);
        Match existing = matchService.getMatchById(id);
        if (existing == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Match not found with id: " + id
            );
        }

        if (!hasAuthority(authentication, "ROLE_ADMIN")) {
            if (!hasAuthority(authentication, "ROLE_ORGANIZER")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Only Organizer or Admin can delete matches");
            }
            assertTournamentOwnership(existing.getTournamentId(), authentication.getName());
        }

        if ("FINISHED".equalsIgnoreCase(existing.getStatus()) || "ONGOING".equalsIgnoreCase(existing.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không thể xóa trận đấu '" + existing.getParticipant1() + " vs " + existing.getParticipant2() + "' vì trận đấu đang diễn ra hoặc đã có kết quả chính thức."
            );
        }
        matchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }

    // CHECK SCHEDULE CONFLICT
    @PostMapping("/check-conflict")
    public ResponseEntity<Map<String, Object>> checkScheduleConflict(
            @RequestBody Match candidate,
            @RequestParam(required = false) Long excludeId) {
        List<Map<String, Object>> conflicts = matchService.checkScheduleConflicts(candidate, excludeId);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("hasConflict", !conflicts.isEmpty());
        res.put("conflictCount", conflicts.size());
        res.put("conflicts", conflicts);
        return ResponseEntity.ok(res);
    }

    // AUTO GENERATE SCHEDULE BY FORMAT
    @PostMapping("/generate-schedule")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> generateSchedule(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        assertAuthenticated(authentication);
        if (request.get("tournamentId") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tournament ID is required");
        }
        Long tournamentId = Long.valueOf(String.valueOf(request.get("tournamentId")));

        if (!hasAuthority(authentication, "ROLE_ADMIN")) {
            if (!hasAuthority(authentication, "ROLE_ORGANIZER")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Only Organizer or Admin can generate schedule");
            }
            assertTournamentOwnership(tournamentId, authentication.getName());
        } else {
            assertTournamentExists(tournamentId);
        }

        Long competitionId = request.get("competitionId") != null ? Long.valueOf(String.valueOf(request.get("competitionId"))) : 1L;
        String format = request.get("format") != null ? String.valueOf(request.get("format")) : "ROUND_ROBIN";
        String startDate = request.get("startDate") != null ? String.valueOf(request.get("startDate")) : null;
        List<String> teams = (List<String>) request.get("teams");
        List<String> venues = (List<String>) request.get("venues");
        List<String> referees = (List<String>) request.get("referees");

        List<Match> generated = matchService.generateSchedule(
                tournamentId, competitionId, format, teams, startDate, venues, referees, authentication.getName());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("tournamentId", tournamentId);
        res.put("format", format);
        res.put("generatedCount", generated.size());
        res.put("matches", generated);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    private void verifyMatchUpdatePermission(
            Match existing,
            Long targetTournamentId,
            Authentication auth,
            boolean isPatch,
            Match putCandidate,
            Map<String, Object> patchUpdates) {

        if (hasAuthority(auth, "ROLE_ADMIN")) {
            assertTournamentExists(targetTournamentId);
            return;
        }

        if (hasAuthority(auth, "ROLE_INTERNAL_RESULT_SYNC")) {
            if (!isPatch || patchUpdates == null || patchUpdates.size() != 1 || !patchUpdates.containsKey("status")) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Access Denied: Internal result-sync scope may only PATCH match status."
                );
            }
            Object rawStatus = patchUpdates.get("status");
            if (rawStatus == null || !"FINISHED".equalsIgnoreCase(String.valueOf(rawStatus).trim())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Access Denied: Internal result-sync scope may only transition match status to FINISHED."
                );
            }
            return;
        }

        if (hasAuthority(auth, "ROLE_ORGANIZER")) {
            assertTournamentOwnership(existing.getTournamentId(), auth.getName());
            if (!Objects.equals(existing.getTournamentId(), targetTournamentId)) {
                assertTournamentOwnership(targetTournamentId, auth.getName());
            }
            return;
        }

        if (hasAuthority(auth, "ROLE_REFEREE")) {
            String assignedRefereeUsername = existing.getRefereeUsername();
            if (assignedRefereeUsername == null || assignedRefereeUsername.isBlank()
                    || !assignedRefereeUsername.equalsIgnoreCase(auth.getName())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Access Denied: Trận đấu này không được phân công cho tài khoản trọng tài của bạn."
                );
            }

            Set<String> allowedRefereeStatuses = Set.of("SCHEDULED", "ONGOING", "FINISHED");
            if (isPatch) {
                if (patchUpdates != null) {
                    for (String key : patchUpdates.keySet()) {
                        if (!"status".equals(key)) {
                            throw new ResponseStatusException(
                                    HttpStatus.FORBIDDEN,
                                    "Access Denied: Trọng tài chỉ được phép cập nhật trạng thái trận đấu được phân công, không được sửa trường: " + key
                            );
                        }
                    }
                    Object rawStatus = patchUpdates.get("status");
                    if (rawStatus != null && !allowedRefereeStatuses.contains(String.valueOf(rawStatus).trim().toUpperCase())) {
                        throw new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Access Denied: Trọng tài không được phép đặt trạng thái: " + rawStatus
                        );
                    }
                }
            } else if (putCandidate != null) {
                boolean modifiedProtectedFields = !Objects.equals(existing.getTournamentId(), putCandidate.getTournamentId())
                        || (putCandidate.getCompetitionId() != null && !Objects.equals(existing.getCompetitionId(), putCandidate.getCompetitionId()))
                        || !Objects.equals(normStr(existing.getRound()), normStr(putCandidate.getRound()))
                        || !Objects.equals(normStr(existing.getGroupName()), normStr(putCandidate.getGroupName()))
                        || !Objects.equals(existing.getMatchTime(), putCandidate.getMatchTime())
                        || !Objects.equals(normStr(existing.getLocation()), normStr(putCandidate.getLocation()))
                        || !Objects.equals(normStr(existing.getReferee()), normStr(putCandidate.getReferee()))
                        || !Objects.equals(normStr(existing.getRefereeUsername()), normStr(putCandidate.getRefereeUsername()))
                        || !Objects.equals(normStr(existing.getParticipant1()), normStr(putCandidate.getParticipant1()))
                        || !Objects.equals(normStr(existing.getParticipant2()), normStr(putCandidate.getParticipant2()));
                if (modifiedProtectedFields) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Access Denied: Trọng tài chỉ được phép cập nhật trạng thái trận đấu được phân công, không được thay đổi giải đấu, thời gian, sân, đội hoặc phân công trọng tài."
                    );
                }
                if (putCandidate.getStatus() == null
                        || !allowedRefereeStatuses.contains(putCandidate.getStatus().trim().toUpperCase())) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Access Denied: Trạng thái trận đấu không hợp lệ đối với quyền trọng tài."
                    );
                }
            }
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not have permission to modify matches");
    }

    private void assertAuthenticated(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null || auth.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Full authentication is required to modify matches");
        }
    }

    private boolean hasAuthority(Authentication auth, String role) {
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> role.equalsIgnoreCase(a.getAuthority()));
    }

    private void assertTournamentExists(Long tournamentId) {
        Map<String, Object> tour = tournamentClient.getTournamentById(tournamentId);
        if (tour == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tournament ID: " + tournamentId);
        }
    }

    private void assertTournamentOwnership(Long tournamentId, String username) {
        Map<String, Object> tour = tournamentClient.getTournamentById(tournamentId);
        if (tour == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tournament ID: " + tournamentId);
        }
        Object ownerObj = tour.get("organizerUsername");
        String owner = ownerObj != null ? String.valueOf(ownerObj) : null;
        if (owner == null || owner.isBlank() || !owner.equalsIgnoreCase(username)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access Denied: Bạn không phải ban tổ chức quản lý giải đấu #" + tournamentId
            );
        }
    }

    private String normStr(String val) {
        return val == null || val.isBlank() ? null : val.trim();
    }
}
