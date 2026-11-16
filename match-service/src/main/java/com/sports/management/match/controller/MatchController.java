package com.sports.management.match.controller;

import com.sports.management.match.client.TournamentClient;
import com.sports.management.match.model.Match;
import com.sports.management.match.service.MatchService;
import jakarta.validation.Valid;
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
            @Valid @RequestBody Match match,
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
            @Valid @RequestBody Match match,
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
        if (request == null || request.get("tournamentId") == null || String.valueOf(request.get("tournamentId")).isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tournament ID is required");
        }
        if (request.get("competitionId") == null || String.valueOf(request.get("competitionId")).isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competition ID is required");
        }

        Long tournamentId;
        Long competitionId;
        try {
            tournamentId = Long.valueOf(String.valueOf(request.get("tournamentId")));
            competitionId = Long.valueOf(String.valueOf(request.get("competitionId")));
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tournamentId or competitionId");
        }

        if (!hasAuthority(authentication, "ROLE_ADMIN")) {
            if (!hasAuthority(authentication, "ROLE_ORGANIZER")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Only Organizer or Admin can generate schedule");
            }
            assertTournamentOwnership(tournamentId, authentication.getName());
        } else {
            assertTournamentExists(tournamentId);
        }

        String clientFormat = request.get("format") != null
                ? String.valueOf(request.get("format"))
                : (request.get("formatType") != null ? String.valueOf(request.get("formatType")) : null);
        String startDate = request.get("startDate") != null ? String.valueOf(request.get("startDate")) : null;
        List<String> venues = (List<String>) request.get("venues");
        List<String> referees = (List<String>) request.get("referees");

        MatchService.ScheduleGenerationResult result = matchService.generateScheduleFromParticipants(
                tournamentId, competitionId, clientFormat, startDate, venues, referees, authentication.getName());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("tournamentId", tournamentId);
        res.put("competitionId", competitionId);
        res.put("format", result.formatType());
        res.put("generatedCount", result.matches().size());
        res.put("matches", result.matches());
        res.put("byeParticipants", result.byeParticipants());
        res.put("message", result.message());
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    // AUTO GENERATE NEXT KNOCKOUT ROUND (D1)
    @PostMapping("/generate-next-round")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> generateNextRound(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        assertAuthenticated(authentication);
        if (request == null || request.get("competitionId") == null || String.valueOf(request.get("competitionId")).isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competition ID is required");
        }
        Long competitionId;
        try {
            competitionId = Long.valueOf(String.valueOf(request.get("competitionId")));
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid competitionId");
        }

        Map<String, Object> comp;
        try {
            comp = tournamentClient.getCompetitionById(competitionId);
        } catch (com.sports.management.exception.ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found with id: " + competitionId);
        }
        if (comp == null || comp.get("tournamentId") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competition #" + competitionId + " has no parent tournamentId");
        }
        Long tournamentId = Long.valueOf(String.valueOf(comp.get("tournamentId")));

        if (!hasAuthority(authentication, "ROLE_ADMIN")) {
            if (!hasAuthority(authentication, "ROLE_ORGANIZER")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Only Organizer or Admin can generate next round");
            }
            assertTournamentOwnership(tournamentId, authentication.getName());
        } else {
            assertTournamentExists(tournamentId);
        }

        String startDate = request.get("startDate") != null ? String.valueOf(request.get("startDate")) : null;
        List<String> venues = (List<String>) request.get("venues");
        List<String> referees = (List<String>) request.get("referees");

        MatchService.NextRoundGenerationResult result = matchService.generateNextKnockoutRound(
                competitionId, startDate, venues, referees, authentication.getName());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("tournamentId", result.tournamentId());
        res.put("competitionId", result.competitionId());
        res.put("round", result.roundLabel());
        res.put("format", "KNOCKOUT");
        res.put("generatedCount", result.matches().size());
        res.put("matches", result.matches());
        res.put("byeParticipants", result.byeParticipants());
        res.put("thirdPlaceGenerated", result.thirdPlaceGenerated());
        res.put("message", result.message());
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    private void verifyMatchUpdatePermission(
            Match existing,
            Long targetTournamentId,
            Authentication auth,
            boolean isPatch,
            Match putCandidate,
            Map<String, Object> patchUpdates) {

        String requestedStatus = resolveRequestedStatus(isPatch, putCandidate, patchUpdates);

        if (hasAuthority(auth, "ROLE_ADMIN")) {
            if ("FINISHED".equalsIgnoreCase(requestedStatus)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Access Denied: Không được đặt trạng thái trận đấu sang FINISHED trực tiếp; trận chỉ kết thúc khi kết quả được xác nhận chính thức."
                );
            }
            assertTournamentExists(targetTournamentId);
            return;
        }

        if (hasAuthority(auth, "ROLE_INTERNAL_RESULT_SYNC")) {
            Set<String> allowedInternalKeys = Set.of(
                    "status", "scoreParticipant1", "scoreParticipant2", "penaltyScore1", "penaltyScore2"
            );
            if (!isPatch || patchUpdates == null || !patchUpdates.containsKey("status")) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Access Denied: Internal result-sync scope may only PATCH match status and final score."
                );
            }
            for (String key : patchUpdates.keySet()) {
                if (!allowedInternalKeys.contains(key)) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Access Denied: Internal result-sync scope may not modify field: " + key
                    );
                }
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
            if ("FINISHED".equalsIgnoreCase(requestedStatus)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Access Denied: Ban tổ chức không được đặt trạng thái trận đấu sang FINISHED trực tiếp; trận chỉ kết thúc khi kết quả được xác nhận chính thức."
                );
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

            String currentStatus = existing.getStatus() != null ? existing.getStatus().trim().toUpperCase(java.util.Locale.ROOT) : "SCHEDULED";
            boolean canStartFromCurrent = "SCHEDULED".equals(currentStatus) || "POSTPONED".equals(currentStatus);

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
                    String nextStatus = rawStatus != null ? String.valueOf(rawStatus).trim().toUpperCase(java.util.Locale.ROOT) : "";
                    if (!canStartFromCurrent || !"ONGOING".equals(nextStatus)) {
                        throw new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Access Denied: Trọng tài chỉ được chuyển trận đấu từ SCHEDULED/POSTPONED sang ONGOING (không được đặt FINISHED trực tiếp)."
                        );
                    }
                }
            } else if (putCandidate != null) {
                boolean modifiedProtectedFields = !Objects.equals(existing.getTournamentId(), putCandidate.getTournamentId())
                        || !Objects.equals(existing.getCompetitionId(), putCandidate.getCompetitionId())
                        || !Objects.equals(existing.getParticipant1Id(), putCandidate.getParticipant1Id())
                        || !Objects.equals(existing.getParticipant2Id(), putCandidate.getParticipant2Id())
                        || !Objects.equals(normStr(existing.getRound()), normStr(putCandidate.getRound()))
                        || !Objects.equals(normStr(existing.getGroupName()), normStr(putCandidate.getGroupName()))
                        || !Objects.equals(existing.getMatchTime(), putCandidate.getMatchTime())
                        || !Objects.equals(normStr(existing.getLocation()), normStr(putCandidate.getLocation()))
                        || !Objects.equals(normStr(existing.getReferee()), normStr(putCandidate.getReferee()))
                        || !Objects.equals(normStr(existing.getRefereeUsername()), normStr(putCandidate.getRefereeUsername()))
                        || (normStr(putCandidate.getParticipant1()) != null && !Objects.equals(normStr(existing.getParticipant1()), normStr(putCandidate.getParticipant1())))
                        || (normStr(putCandidate.getParticipant2()) != null && !Objects.equals(normStr(existing.getParticipant2()), normStr(putCandidate.getParticipant2())));
                if (modifiedProtectedFields) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Access Denied: Trọng tài chỉ được phép cập nhật trạng thái trận đấu được phân công, không được thay đổi giải đấu, thời gian, sân, đội hoặc phân công trọng tài."
                    );
                }
                String nextStatus = putCandidate.getStatus() != null ? putCandidate.getStatus().trim().toUpperCase(java.util.Locale.ROOT) : "";
                if (!canStartFromCurrent || !"ONGOING".equals(nextStatus)) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Access Denied: Trọng tài chỉ được chuyển trận đấu từ SCHEDULED/POSTPONED sang ONGOING."
                    );
                }
            }
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not have permission to modify matches");
    }

    private String resolveRequestedStatus(boolean isPatch, Match putCandidate, Map<String, Object> patchUpdates) {
        if (isPatch) {
            if (patchUpdates != null && patchUpdates.get("status") != null) {
                return String.valueOf(patchUpdates.get("status")).trim();
            }
            return null;
        }
        return putCandidate != null && putCandidate.getStatus() != null ? putCandidate.getStatus().trim() : null;
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
