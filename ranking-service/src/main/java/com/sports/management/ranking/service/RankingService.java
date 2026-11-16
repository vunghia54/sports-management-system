package com.sports.management.ranking.service;

import com.sports.management.exception.ResourceNotFoundException;
import com.sports.management.ranking.client.RankingProviderClient;
import com.sports.management.ranking.model.Ranking;
import com.sports.management.ranking.model.RankingItem;
import com.sports.management.ranking.model.RankingResponse;
import com.sports.management.ranking.repository.RankingItemRepository;
import com.sports.management.ranking.repository.RankingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RankingService {

    private final RankingRepository rankingRepository;
    private final RankingItemRepository rankingItemRepository;
    private final RankingProviderClient rankingProviderClient;

    public RankingService(
            RankingRepository rankingRepository,
            RankingItemRepository rankingItemRepository,
            RankingProviderClient rankingProviderClient) {
        this.rankingRepository = rankingRepository;
        this.rankingItemRepository = rankingItemRepository;
        this.rankingProviderClient = rankingProviderClient;
    }

    public List<Ranking> getAllRankings() {
        return rankingRepository.findAll();
    }

    public List<RankingResponse> getAllRankingsWithDetails() {
        return rankingRepository.findAll().stream()
                .map(r -> new RankingResponse(r, rankingItemRepository.findByRankingIdOrderByRankPositionAsc(r.getId())))
                .toList();
    }

    public Page<Ranking> searchRankings(Long competitionId, String keyword, Pageable pageable) {
        String safeKw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return rankingRepository.searchRankings(competitionId, safeKw, pageable);
    }

    public Map<String, Object> getRankingStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalRankings", rankingRepository.count());
        stats.put("totalRankingItems", rankingItemRepository.count());
        return stats;
    }

    public Ranking getRankingById(Long id) {
        return rankingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ranking not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public RankingResponse getRankingByCompetition(Long competitionId) {
        return getRankingByCompetition(competitionId, false);
    }

    @Transactional(readOnly = true)
    public RankingResponse getRankingByCompetition(Long competitionId, boolean createIfMissingIgnored) {
        Optional<Ranking> existing = rankingRepository.findByCompetitionId(competitionId);
        if (existing.isEmpty()) {
            return new RankingResponse(null, Collections.emptyList());
        }
        Ranking ranking = existing.get();
        List<RankingItem> items = rankingItemRepository.findByRankingIdOrderByRankPositionAsc(ranking.getId());
        return new RankingResponse(ranking, items);
    }

    @Transactional(readOnly = true)
    public Page<RankingItem> searchRankingItemsByCompetition(Long competitionId, String keyword, Pageable pageable) {
        Optional<Ranking> existing = rankingRepository.findByCompetitionId(competitionId);
        if (existing.isEmpty()) {
            return Page.empty(pageable);
        }
        String safeKw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return rankingItemRepository.searchItems(existing.get().getId(), safeKw, pageable);
    }

    @Transactional
    public Ranking createDefaultRanking(Long competitionId) {
        Map<String, Object> comp = null;
        try {
            comp = rankingProviderClient.getCompetitionById(competitionId);
        } catch (Exception ignored) {
        }
        return createDefaultRanking(competitionId, comp);
    }

    private Ranking createDefaultRanking(Long competitionId, Map<String, Object> comp) {
        String compName = (comp != null && comp.get("competitionName") != null)
                ? String.valueOf(comp.get("competitionName"))
                : (comp != null && comp.get("name") != null
                    ? String.valueOf(comp.get("name"))
                    : ("Nội dung " + competitionId));

        Ranking ranking = new Ranking();
        ranking.setCompetitionId(competitionId);
        ranking.setRankingName("Bảng xếp hạng - " + compName);
        ranking.setUpdatedAt(LocalDateTime.now());
        return rankingRepository.save(ranking);
    }

    @Transactional
    public RankingResponse recalculateRanking(Long competitionId) {
        // 1. Verify competition exists (404 if absent) and check format != KNOCKOUT before mutating DB
        Map<String, Object> competition = rankingProviderClient.getCompetitionById(competitionId);
        if (competition == null) {
            throw new ResourceNotFoundException("Competition not found with id: " + competitionId);
        }
        Map<String, Object> format = rankingProviderClient.getCompetitionFormat(competitionId);
        if (isKnockoutFormat(format)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nội dung thi đấu thể thức loại trực tiếp (KNOCKOUT) không áp dụng bảng xếp hạng tính điểm");
        }

        // 2. Load matches of competitionId (skipping CANCELLED) and load results only by matchIds
        List<Map<String, Object>> rawMatches = rankingProviderClient.getMatchesByCompetitionId(competitionId);
        List<Map<String, Object>> matches = rawMatches.stream()
                .filter(m -> m.get("competitionId") == null || competitionId.equals(Long.valueOf(String.valueOf(m.get("competitionId")))))
                .filter(m -> !"CANCELLED".equalsIgnoreCase(String.valueOf(m.get("status"))))
                .toList();

        List<Long> matchIds = matches.stream()
                .map(m -> m.get("id"))
                .filter(Objects::nonNull)
                .map(v -> Long.valueOf(String.valueOf(v)))
                .distinct()
                .toList();

        List<Map<String, Object>> batchResults = matchIds.isEmpty()
                ? Collections.emptyList()
                : rankingProviderClient.getResultsByMatchIds(matchIds);

        Ranking ranking = rankingRepository.findByCompetitionId(competitionId)
                .orElseGet(() -> createDefaultRanking(competitionId, competition));

        Map<Long, Map<String, Object>> resultByMatchId = new HashMap<>();
        for (Map<String, Object> r : batchResults) {
            if (r.get("matchId") != null) {
                Long mId = Long.valueOf(String.valueOf(r.get("matchId")));
                resultByMatchId.put(mId, r);
            }
        }

        Map<String, RankingItem> statsMap = new LinkedHashMap<>();

        for (Map<String, Object> match : matches) {
            Long p1Id = match.get("participant1Id") != null ? Long.valueOf(String.valueOf(match.get("participant1Id"))) : null;
            Long p2Id = match.get("participant2Id") != null ? Long.valueOf(String.valueOf(match.get("participant2Id"))) : null;
            String p1 = match.get("participant1") != null ? String.valueOf(match.get("participant1")) : null;
            String p2 = match.get("participant2") != null ? String.valueOf(match.get("participant2")) : null;
            Long matchId = match.get("id") != null ? Long.valueOf(String.valueOf(match.get("id"))) : null;

            String key1 = resolveParticipantKey(p1Id, p1);
            String key2 = resolveParticipantKey(p2Id, p2);

            if (key1 != null) {
                RankingItem item1 = statsMap.computeIfAbsent(key1, k -> {
                    RankingItem item = new RankingItem(null, ranking.getId(), 0,
                            (p1 != null && !p1.isBlank()) ? p1 : ("Participant #" + p1Id),
                            0, 0, 0, 0, 0, 0, 0, 0);
                    item.setParticipantId(p1Id);
                    return item;
                });
                if (item1.getParticipantId() == null && p1Id != null) {
                    item1.setParticipantId(p1Id);
                }
            }
            if (key2 != null) {
                RankingItem item2 = statsMap.computeIfAbsent(key2, k -> {
                    RankingItem item = new RankingItem(null, ranking.getId(), 0,
                            (p2 != null && !p2.isBlank()) ? p2 : ("Participant #" + p2Id),
                            0, 0, 0, 0, 0, 0, 0, 0);
                    item.setParticipantId(p2Id);
                    return item;
                });
                if (item2.getParticipantId() == null && p2Id != null) {
                    item2.setParticipantId(p2Id);
                }
            }

            if (matchId != null && resultByMatchId.containsKey(matchId)) {
                Map<String, Object> res = resultByMatchId.get(matchId);
                String resStatus = res.get("status") != null ? String.valueOf(res.get("status")) : "";

                // Chi tinh ket qua da xac nhan chinh thuc (CONFIRMED)
                if ("CONFIRMED".equalsIgnoreCase(resStatus)) {
                    RankingItem item1 = key1 != null ? statsMap.get(key1) : null;
                    RankingItem item2 = key2 != null ? statsMap.get(key2) : null;

                    int s1 = res.get("scoreParticipant1") != null
                            ? Integer.parseInt(String.valueOf(res.get("scoreParticipant1"))) : 0;
                    int s2 = res.get("scoreParticipant2") != null
                            ? Integer.parseInt(String.valueOf(res.get("scoreParticipant2"))) : 0;

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

        List<RankingItem> items = new ArrayList<>(statsMap.values());
        for (RankingItem item : items) {
            item.setGoalDifference(item.getGoalsFor() - item.getGoalsAgainst());
        }

        items.sort((a, b) -> {
            int cmpPoints = Integer.compare(b.getPoints(), a.getPoints());
            if (cmpPoints != 0) return cmpPoints;
            int cmpDiff = Integer.compare(b.getGoalDifference(), a.getGoalDifference());
            if (cmpDiff != 0) return cmpDiff;
            int cmpGoals = Integer.compare(b.getGoalsFor(), a.getGoalsFor());
            if (cmpGoals != 0) return cmpGoals;
            String n1 = a.getParticipantName() != null ? a.getParticipantName() : "";
            String n2 = b.getParticipantName() != null ? b.getParticipantName() : "";
            return String.CASE_INSENSITIVE_ORDER.compare(n1, n2);
        });

        rankingItemRepository.deleteByRankingId(ranking.getId());

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

    private boolean isKnockoutFormat(Map<String, Object> format) {
        if (format == null || format.get("formatType") == null) {
            return false;
        }
        String fmt = String.valueOf(format.get("formatType")).trim().toUpperCase(Locale.ROOT);
        return "KNOCKOUT".equals(fmt) || "SINGLE_ELIMINATION".equals(fmt);
    }

    private String resolveParticipantKey(Long participantId, String displayName) {
        if (participantId != null) {
            return "ID:" + participantId;
        }
        if (displayName != null && !displayName.isBlank()) {
            return "NAME:" + displayName.trim().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    @Transactional
    public void deleteRanking(Long id) {
        Ranking existing = getRankingById(id);
        rankingItemRepository.deleteByRankingId(existing.getId());
        rankingRepository.delete(existing);
    }
}
