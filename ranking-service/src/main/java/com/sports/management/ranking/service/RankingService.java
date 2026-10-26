package com.sports.management.ranking.service;

import com.sports.management.ranking.model.Ranking;
import com.sports.management.ranking.model.RankingItem;
import com.sports.management.ranking.model.RankingResponse;
import com.sports.management.ranking.repository.RankingItemRepository;
import com.sports.management.ranking.repository.RankingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RankingService {

    private final RankingRepository rankingRepository;
    private final RankingItemRepository rankingItemRepository;

    public RankingService(
            RankingRepository rankingRepository,
            RankingItemRepository rankingItemRepository) {
        this.rankingRepository = rankingRepository;
        this.rankingItemRepository = rankingItemRepository;
    }

    public List<Ranking> getAllRankings() {
        return rankingRepository.findAll();
    }

    public RankingResponse getRankingByCompetition(Long competitionId) {
        Ranking ranking = rankingRepository.findByCompetitionId(competitionId)
                .orElseGet(() -> createDefaultRanking(competitionId));

        List<RankingItem> items = rankingItemRepository.findByRankingIdOrderByRankPositionAsc(ranking.getId());
        return new RankingResponse(ranking, items);
    }

    @Transactional
    public Ranking createDefaultRanking(Long competitionId) {
        Ranking ranking = new Ranking();
        ranking.setCompetitionId(competitionId);
        ranking.setRankingName("Bảng xếp hạng nội dung " + competitionId);
        ranking.setUpdatedAt(LocalDateTime.now());
        return rankingRepository.save(ranking);
    }

    @Transactional
    public RankingResponse updateRankingItems(Long competitionId, List<RankingItem> items) {
        Ranking ranking = rankingRepository.findByCompetitionId(competitionId)
                .orElseGet(() -> createDefaultRanking(competitionId));

        rankingItemRepository.deleteByRankingId(ranking.getId());

        int pos = 1;
        for (RankingItem item : items) {
            item.setId(null);
            item.setRankingId(ranking.getId());
            item.setRankPosition(pos++);
            item.setGoalDifference(item.getGoalsFor() - item.getGoalsAgainst());
        }
        List<RankingItem> saved = rankingItemRepository.saveAll(items);

        ranking.setUpdatedAt(LocalDateTime.now());
        rankingRepository.save(ranking);

        return new RankingResponse(ranking, saved);
    }
}
