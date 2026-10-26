package com.sports.management.ranking.controller;

import com.sports.management.ranking.model.Ranking;
import com.sports.management.ranking.model.RankingItem;
import com.sports.management.ranking.model.RankingResponse;
import com.sports.management.ranking.service.RankingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rankings")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping
    public ResponseEntity<List<Ranking>> getAllRankings() {
        return ResponseEntity.ok(rankingService.getAllRankings());
    }

    @GetMapping("/competition/{competitionId}")
    public ResponseEntity<RankingResponse> getRankingByCompetition(@PathVariable Long competitionId) {
        return ResponseEntity.ok(rankingService.getRankingByCompetition(competitionId));
    }

    @PutMapping("/competition/{competitionId}/items")
    public ResponseEntity<RankingResponse> updateRankingItems(
            @PathVariable Long competitionId,
            @RequestBody List<RankingItem> items) {
        return ResponseEntity.ok(rankingService.updateRankingItems(competitionId, items));
    }
}
