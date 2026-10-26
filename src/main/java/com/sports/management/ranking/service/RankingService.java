package com.sports.management.ranking.service;

import com.sports.management.exception.ResourceNotFoundException;
import com.sports.management.match.model.Match;
import com.sports.management.match.repository.MatchRepository;
import com.sports.management.ranking.model.Ranking;
import com.sports.management.ranking.model.RankingItem;
import com.sports.management.ranking.model.RankingResponse;
import com.sports.management.ranking.repository.RankingItemRepository;
import com.sports.management.ranking.repository.RankingRepository;
import com.sports.management.result.model.Result;
import com.sports.management.result.repository.ResultRepository;
import com.sports.management.tournament.model.Competition;
import com.sports.management.tournament.service.CompetitionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RankingService {

    private final RankingRepository rankingRepository;
    private final RankingItemRepository rankingItemRepository;
    private final MatchRepository matchRepository;
    private final ResultRepository resultRepository;
    private final CompetitionService competitionService;

    public RankingService(
            RankingRepository rankingRepository,
            RankingItemRepository rankingItemRepository,
            MatchRepository matchRepository,
            ResultRepository resultRepository,
            CompetitionService competitionService) {
        this.rankingRepository = rankingRepository;
        this.rankingItemRepository = rankingItemRepository;
        this.matchRepository = matchRepository;
        this.resultRepository = resultRepository;
        this.competitionService = competitionService;
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
        Competition competition = competitionService.getCompetitionById(competitionId);
        Ranking ranking = new Ranking();
        ranking.setCompetitionId(competitionId);
        ranking.setRankingName("Bảng xếp hạng - " + competition.getCompetitionName());
        ranking.setUpdatedAt(LocalDateTime.now());
        return rankingRepository.save(ranking);
    }

    @Transactional
    public RankingResponse recalculateRanking(Long competitionId) {
        Ranking ranking = rankingRepository.findByCompetitionId(competitionId)
                .orElseGet(() -> createDefaultRanking(competitionId));

        // Lay tat ca tran dau cua noi dung thi dau
        List<Match> matches = matchRepository.findByCompetitionId(competitionId);
        if (matches.isEmpty()) {
            // Neu tran dau chua gan competitionId, thu tim theo tournamentId
            Competition comp = competitionService.getCompetitionById(competitionId);
            if (comp != null && comp.getTournamentId() != null) {
                matches = matchRepository.findByTournamentId(comp.getTournamentId());
            }
        }

        // Map ten doi tuong -> RankingItem
        Map<String, RankingItem> statsMap = new HashMap<>();

        for (Match match : matches) {
            String p1 = match.getParticipant1();
            String p2 = match.getParticipant2();

            if (p1 != null && !p1.isBlank()) {
                statsMap.putIfAbsent(p1, new RankingItem(null, ranking.getId(), 0, p1, 0, 0, 0, 0, 0, 0, 0, 0));
            }
            if (p2 != null && !p2.isBlank()) {
                statsMap.putIfAbsent(p2, new RankingItem(null, ranking.getId(), 0, p2, 0, 0, 0, 0, 0, 0, 0, 0));
            }

            // Chi tinh diem cho cac tran da co ket qua xac nhan (hoac da FINISHED)
            Optional<Result> resultOpt = resultRepository.findByMatchId(match.getId());
            if (resultOpt.isPresent()) {
                Result res = resultOpt.get();
                if ("CONFIRMED".equalsIgnoreCase(res.getStatus()) || "FINISHED".equalsIgnoreCase(match.getStatus())) {
                    RankingItem item1 = statsMap.get(p1);
                    RankingItem item2 = statsMap.get(p2);

                    int s1 = res.getScoreParticipant1() != null ? res.getScoreParticipant1() : 0;
                    int s2 = res.getScoreParticipant2() != null ? res.getScoreParticipant2() : 0;

                    if (item1 != null) {
                        item1.setMatchesPlayed(item1.getMatchesPlayed() + 1);
                        item1.setGoalsFor(item1.getGoalsFor() + s1);
                        item1.setGoalsAgainst(item1.getGoalsAgainst() + s2);
                    }
                    if (item2 != null) {
                        item2.setMatchesPlayed(item2.getMatchesPlayed() + 1);
                        item2.setGoalsFor(item2.getGoalsFor() + s2);
                        item2.setGoalsAgainst(item2.getGoalsAgainst() + s1);
                    }

                    if (s1 > s2) {
                        if (item1 != null) {
                            item1.setWon(item1.getWon() + 1);
                            item1.setPoints(item1.getPoints() + 3);
                        }
                        if (item2 != null) {
                            item2.setLost(item2.getLost() + 1);
                        }
                    } else if (s2 > s1) {
                        if (item2 != null) {
                            item2.setWon(item2.getWon() + 1);
                            item2.setPoints(item2.getPoints() + 3);
                        }
                        if (item1 != null) {
                            item1.setLost(item1.getLost() + 1);
                        }
                    } else {
                        // Hoa
                        if (item1 != null) {
                            item1.setDrawn(item1.getDrawn() + 1);
                            item1.setPoints(item1.getPoints() + 1);
                        }
                        if (item2 != null) {
                            item2.setDrawn(item2.getDrawn() + 1);
                            item2.setPoints(item2.getPoints() + 1);
                        }
                    }
                }
            }
        }

        // Tinh goal difference va sap xep
        List<RankingItem> items = new ArrayList<>(statsMap.values());
        for (RankingItem item : items) {
            item.setGoalDifference(item.getGoalsFor() - item.getGoalsAgainst());
        }

        // Sap xep: Diem cao -> Hieu so cao -> Ban thang cao
        items.sort((a, b) -> {
            int cmpPoints = Integer.compare(b.getPoints(), a.getPoints());
            if (cmpPoints != 0) return cmpPoints;
            int cmpDiff = Integer.compare(b.getGoalDifference(), a.getGoalDifference());
            if (cmpDiff != 0) return cmpDiff;
            return Integer.compare(b.getGoalsFor(), a.getGoalsFor());
        });

        // Xoa du lieu cu cua ranking nay
        rankingItemRepository.deleteByRankingId(ranking.getId());

        // Cap nhat vi tri thu hang va luu lai
        int position = 1;
        for (RankingItem item : items) {
            item.setRankPosition(position++);
            item.setRankingId(ranking.getId());
        }
        List<RankingItem> savedItems = rankingItemRepository.saveAll(items);

        ranking.setUpdatedAt(LocalDateTime.now());
        Ranking savedRanking = rankingRepository.save(ranking);

        return new RankingResponse(savedRanking, savedItems);
    }
}
