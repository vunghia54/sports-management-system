package com.sports.management.ranking.controller;

import com.sports.management.ranking.client.RankingProviderClient;
import com.sports.management.ranking.model.Ranking;
import com.sports.management.ranking.model.RankingItem;
import com.sports.management.ranking.model.RankingResponse;
import com.sports.management.ranking.service.RankingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rankings")
public class RankingController {

    private final RankingService rankingService;
    private final RankingProviderClient rankingProviderClient;
    private final String instanceId;

    public RankingController(
            RankingService rankingService,
            RankingProviderClient rankingProviderClient,
            @Value("${eureka.instance.instance-id:${spring.application.name}:${server.port}}") String instanceId) {
        this.rankingService = rankingService;
        this.rankingProviderClient = rankingProviderClient;
        this.instanceId = instanceId;
    }

    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> sayHello() {
        return ResponseEntity.ok(Map.of(
                "service", "Ranking Service",
                "message", "Welcome to Ranking Service API (TH1 - TH7)",
                "instanceId", instanceId
        ));
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("serviceName", "RANKING-SERVICE");
        info.put("version", "v1");
        info.put("description", "Quản lý bảng xếp hạng và tự động tính điểm liên thông Match Service, Result Service và Tournament Service");
        info.put("instanceId", instanceId);
        return ResponseEntity.ok(info);
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getRankingStatistics() {
        return ResponseEntity.ok(rankingService.getRankingStatistics());
    }

    @GetMapping("/discovery/providers")
    public ResponseEntity<Map<String, Object>> discoverProviders() {
        return ResponseEntity.ok(rankingProviderClient.discoverProviders());
    }

    @GetMapping
    public ResponseEntity<List<Ranking>> getAllRankings() {
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(rankingService.getAllRankings());
    }

    @GetMapping("/details")
    public ResponseEntity<List<RankingResponse>> getAllRankingsWithDetails() {
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(rankingService.getAllRankingsWithDetails());
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<Ranking>> searchRankings(
            @RequestParam(required = false) Long competitionId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = "desc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(rankingService.searchRankings(competitionId, keyword, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ranking> getRankingById(@PathVariable Long id) {
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(rankingService.getRankingById(id));
    }

    @GetMapping("/competition/{competitionId}")
    public ResponseEntity<RankingResponse> getRankingByCompetition(
            @PathVariable Long competitionId,
            @RequestParam(defaultValue = "false") boolean createIfMissing) {
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(rankingService.getRankingByCompetition(competitionId, false));
    }

    @GetMapping("/competition/{competitionId}/items/paged")
    public ResponseEntity<Page<RankingItem>> getRankingItemsPagedByCompetition(
            @PathVariable Long competitionId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "rankPosition") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = "desc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), sort);
        return ResponseEntity.ok(rankingService.searchRankingItemsByCompetition(competitionId, keyword, pageable));
    }

    @PostMapping("/competition/{competitionId}/recalculate")
    public ResponseEntity<RankingResponse> recalculateRanking(@PathVariable Long competitionId) {
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(rankingService.recalculateRanking(competitionId));
    }

    @PutMapping("/competition/{competitionId}/items")
    public ResponseEntity<RankingResponse> updateRankingItems(
            @PathVariable Long competitionId,
            @RequestBody List<RankingItem> items) {
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(rankingService.updateRankingItems(competitionId, items));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRanking(@PathVariable Long id) {
        rankingService.deleteRanking(id);
        return ResponseEntity.noContent().build();
    }
}
