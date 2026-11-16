package com.sports.management.result.controller;

import com.sports.management.result.client.MatchAndRankingClient;
import com.sports.management.result.model.RecordResultRequest;
import com.sports.management.result.model.Result;
import com.sports.management.result.model.ResultResponse;
import com.sports.management.result.service.ResultService;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/results")
public class ResultController {

    private final ResultService resultService;
    private final MatchAndRankingClient matchAndRankingClient;
    private final String instanceId;

    public ResultController(
            ResultService resultService,
            MatchAndRankingClient matchAndRankingClient,
            @Value("${eureka.instance.instance-id:${spring.application.name}:${server.port}}") String instanceId) {
        this.resultService = resultService;
        this.matchAndRankingClient = matchAndRankingClient;
        this.instanceId = instanceId;
    }

    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> sayHello() {
        return ResponseEntity.ok(Map.of(
                "service", "Result Service",
                "message", "Welcome to Result Service API (TH1 - TH7)",
                "instanceId", instanceId
        ));
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("serviceName", "RESULT-SERVICE");
        info.put("version", "v1");
        info.put("description", "Quản lý kết quả trận đấu, chi tiết sự kiện trận đấu và xác nhận kết quả liên thông Match/Ranking Service");
        info.put("instanceId", instanceId);
        return ResponseEntity.ok(info);
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getResultStatistics() {
        return ResponseEntity.ok(resultService.getResultStatistics());
    }

    @GetMapping("/discovery/match-instance")
    public ResponseEntity<Map<String, Object>> discoverMatchInstance() {
        return ResponseEntity.ok(matchAndRankingClient.discoverMatchInstance());
    }

    @PostMapping
    public ResponseEntity<ResultResponse> recordResult(
            @Valid @RequestBody RecordResultRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        assertAuthenticated(authentication);
        Map<String, Object> match = verifyCanRecordOrEditResult(request.getMatchId(), authentication);
        resultService.findByMatchId(request.getMatchId()).ifPresent(existing -> {
            if ("CONFIRMED".equalsIgnoreCase(existing.getStatus())
                    && !hasAuthority(authentication, "ROLE_ADMIN")
                    && !hasAuthority(authentication, "ROLE_ORGANIZER")) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Access Denied: Kết quả đã xác nhận chính thức (CONFIRMED). Trọng tài không được ghi đè; chỉ Ban tổ chức sở hữu giải hoặc Admin được mở lại."
                );
            }
        });
        String authHeader = httpRequest != null ? httpRequest.getHeader("Authorization") : null;
        ResultResponse response = resultService.recordResult(request, match, authHeader);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Instance-Id", instanceId)
                .header("X-Match-Provider-Instance", matchAndRankingClient.getLastHandledMatchInstance())
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<Result>> getAllResults() {
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(resultService.getAllResults());
    }

    @GetMapping("/by-matches")
    public ResponseEntity<List<Result>> getResultsByMatchIds(
            @RequestParam(required = false) List<Long> matchIds) {
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(resultService.getResultsByMatchIds(matchIds));
    }

    @GetMapping("/sync-pending")
    public ResponseEntity<List<Result>> getPendingSyncResults(Authentication authentication) {
        assertAuthenticated(authentication);
        if (!hasAuthority(authentication, "ROLE_ADMIN") && !hasAuthority(authentication, "ROLE_ORGANIZER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Only Organizer or Admin can inspect sync queue");
        }
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(resultService.getPendingOrFailedSyncResults());
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<Result>> searchResults(
            @RequestParam(required = false) Long matchId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String winner,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = "desc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(resultService.searchResults(matchId, status, winner, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result> getResultById(@PathVariable Long id) {
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(resultService.getResultById(id));
    }

    @GetMapping("/match/{matchId}")
    public ResponseEntity<ResultResponse> getResultByMatchId(@PathVariable Long matchId) {
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(resultService.getResultResponseByMatchId(matchId));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<ResultResponse> confirmResult(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        assertAuthenticated(authentication);
        Result existing = resultService.getResultById(id);
        Map<String, Object> match = verifyCanConfirmOrSyncResult(existing.getMatchId(), authentication);

        // Always derive confirmedBy from verified Authentication, never trust client payload
        String confirmedBy = authentication.getName();
        String authHeader = httpRequest != null ? httpRequest.getHeader("Authorization") : null;
        ResultResponse response = resultService.confirmResult(id, confirmedBy, authHeader, match);

        String syncStatus = response.getResult() != null ? response.getResult().getSyncStatus() : "SYNCED";
        HttpStatus httpStatus = "SYNCED".equalsIgnoreCase(syncStatus) ? HttpStatus.OK : HttpStatus.ACCEPTED;

        return ResponseEntity.status(httpStatus)
                .header("X-Instance-Id", instanceId)
                .body(response);
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<ResultResponse> retrySyncResult(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        assertAuthenticated(authentication);
        Result existing = resultService.getResultById(id);
        verifyCanConfirmOrSyncResult(existing.getMatchId(), authentication);

        String authHeader = httpRequest != null ? httpRequest.getHeader("Authorization") : null;
        ResultService.SyncAttemptOutcome outcome = resultService.retrySync(id, authHeader);
        return ResponseEntity
                .status(outcome.inProgressElsewhere() ? HttpStatus.ACCEPTED : HttpStatus.OK)
                .header("X-Instance-Id", instanceId)
                .body(outcome.response());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Result> patchResult(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        assertAuthenticated(authentication);
        Result existing = resultService.getResultById(id);
        verifyCanRecordOrEditResult(existing.getMatchId(), authentication);

        if ("CONFIRMED".equalsIgnoreCase(existing.getStatus())
                && !hasAuthority(authentication, "ROLE_ADMIN")
                && !hasAuthority(authentication, "ROLE_ORGANIZER")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access Denied: Trọng tài không được sửa kết quả đã xác nhận (CONFIRMED)."
            );
        }

        if (updates != null) {
            if (updates.containsKey("status")) {
                Object rawStatus = updates.get("status");
                String nextStatus = rawStatus != null ? String.valueOf(rawStatus).trim().toUpperCase() : "";
                if (hasAuthority(authentication, "ROLE_REFEREE") && !nextStatus.isEmpty()) {
                    if (!"PENDING_CONFIRMATION".equals(nextStatus)
                            || "CONFIRMED".equalsIgnoreCase(existing.getStatus())) {
                        throw new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Access Denied: Trọng tài không được tự xác nhận hoặc thay đổi trạng thái xác nhận của kết quả."
                        );
                    }
                }
                if ("CONFIRMED".equals(nextStatus)) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Không được đặt trạng thái CONFIRMED qua PATCH; vui lòng dùng PUT /api/v1/results/{id}/confirm."
                    );
                }
                if (!nextStatus.isEmpty() && !"PENDING_CONFIRMATION".equals(nextStatus)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái kết quả không hợp lệ: " + nextStatus);
                }
            }
            Set<String> allowedPatchFields = Set.of("notes", "status");
            for (String key : updates.keySet()) {
                if (!allowedPatchFields.contains(key)) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Không được phép chỉnh sửa trường quản trị/đồng bộ qua PATCH: " + key
                    );
                }
            }
        }

        String authHeader = httpRequest != null ? httpRequest.getHeader("Authorization") : null;
        return ResponseEntity.ok(resultService.patchNotesOnly(id, updates, authHeader));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResult(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        assertAuthenticated(authentication);
        if (!hasAuthority(authentication, "ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Only Admin can delete results");
        }
        String authHeader = httpRequest != null ? httpRequest.getHeader("Authorization") : null;
        resultService.deleteResult(id, authHeader);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> verifyCanRecordOrEditResult(Long matchId, Authentication auth) {
        Map<String, Object> match = matchAndRankingClient.getMatchById(matchId);
        if (hasAuthority(auth, "ROLE_ADMIN")) {
            return match;
        }
        if (hasAuthority(auth, "ROLE_ORGANIZER")) {
            assertTournamentOwnership(match, auth.getName());
            return match;
        }
        if (hasAuthority(auth, "ROLE_REFEREE")) {
            String assignedRef = match != null && match.get("refereeUsername") != null
                    ? String.valueOf(match.get("refereeUsername")).trim()
                    : "";
            if (assignedRef.isEmpty() || !assignedRef.equalsIgnoreCase(auth.getName())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Access Denied: Trận đấu này không được phân công cho tài khoản trọng tài của bạn."
                );
            }
            return match;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not have permission to record or edit match results");
    }

    private Map<String, Object> verifyCanConfirmOrSyncResult(Long matchId, Authentication auth) {
        Map<String, Object> match = matchAndRankingClient.getMatchById(matchId);
        if (hasAuthority(auth, "ROLE_ADMIN")) {
            return match;
        }
        if (hasAuthority(auth, "ROLE_ORGANIZER")) {
            assertTournamentOwnership(match, auth.getName());
            return match;
        }
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Access Denied: Chỉ Ban tổ chức sở hữu giải hoặc Quản trị viên mới có quyền xác nhận/đồng bộ kết quả."
        );
    }

    private void assertTournamentOwnership(Map<String, Object> match, String username) {
        Long tournamentId = null;
        if (match != null && match.get("tournamentId") != null) {
            try {
                tournamentId = Long.valueOf(String.valueOf(match.get("tournamentId")));
            } catch (NumberFormatException ignored) {
            }
        }
        if (tournamentId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Cannot verify tournament ownership for match without tournamentId");
        }
        Map<String, Object> tournament = matchAndRankingClient.getTournamentById(tournamentId);
        if (tournament == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Associated tournament not found: " + tournamentId);
        }
        Object ownerObj = tournament.get("organizerUsername");
        String owner = ownerObj != null ? String.valueOf(ownerObj).trim() : "";
        if (owner.isEmpty() || !owner.equalsIgnoreCase(username)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access Denied: Bạn không phải ban tổ chức sở hữu giải đấu #" + tournamentId
            );
        }
    }

    private void assertAuthenticated(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null || auth.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Full authentication is required");
        }
    }

    private boolean hasAuthority(Authentication auth, String role) {
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(a -> role.equalsIgnoreCase(a.getAuthority()));
    }
}
