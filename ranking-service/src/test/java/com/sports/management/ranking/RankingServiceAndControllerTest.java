package com.sports.management.ranking;

import com.sports.management.exception.GlobalExceptionHandler;
import com.sports.management.ranking.client.RankingProviderClient;
import com.sports.management.ranking.controller.RankingController;
import com.sports.management.ranking.model.Ranking;
import com.sports.management.ranking.model.RankingItem;
import com.sports.management.ranking.model.RankingResponse;
import com.sports.management.ranking.repository.RankingItemRepository;
import com.sports.management.ranking.repository.RankingRepository;
import com.sports.management.ranking.service.RankingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RankingServiceAndControllerTest {

    @Mock
    private RankingRepository rankingRepository;

    @Mock
    private RankingItemRepository rankingItemRepository;

    @Mock
    private RankingProviderClient rankingProviderClient;

    private RankingService rankingService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        rankingService = new RankingService(rankingRepository, rankingItemRepository, rankingProviderClient);
        RankingController controller = new RankingController(rankingService, rankingProviderClient, "ranking-service:8086");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getRankingByCompetition_whenCreateIfMissingFalse_doesNotSaveNewRanking() {
        when(rankingRepository.findByCompetitionId(99L)).thenReturn(Optional.empty());

        RankingResponse res = rankingService.getRankingByCompetition(99L, false);
        assertNull(res.getRanking());
        assertTrue(res.getItems().isEmpty());
        verify(rankingRepository, never()).save(any());
    }

    @Test
    void getAllRankingsWithDetails_returnsRankingsWithSortedItems() throws Exception {
        Ranking r = new Ranking(1L, 10L, "BXH Bóng Đá Nam", LocalDateTime.now());
        RankingItem item = new RankingItem(1L, 1L, 1, "CLB Hà Nội", 3, 3, 0, 0, 9, 8, 2, 6);
        when(rankingRepository.findAll()).thenReturn(List.of(r));
        when(rankingItemRepository.findByRankingIdOrderByRankPositionAsc(1L)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/rankings/details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ranking.rankingName").value("BXH Bóng Đá Nam"))
                .andExpect(jsonPath("$[0].items[0].participantName").value("CLB Hà Nội"))
                .andExpect(jsonPath("$[0].items[0].points").value(9));
    }

    @Test
    void searchRankings_normalizesBlankKeyword() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(rankingRepository.searchRankings(eq(10L), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<Ranking> page = rankingService.searchRankings(10L, "   ", pageable);
        assertEquals(0, page.getTotalElements());
        verify(rankingRepository).searchRankings(10L, null, pageable);
    }

    @Test
    void competitionEndpoint_neverCreatesOrSavesEvenWhenCreateIfMissingTrue() throws Exception {
        when(rankingRepository.findByCompetitionId(77L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/rankings/competition/77").param("createIfMissing", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());

        mockMvc.perform(get("/api/v1/rankings/competition/77").param("createIfMissing", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());

        verify(rankingRepository, never()).save(any());
        verify(rankingItemRepository, never()).saveAll(any());
    }

    @Test
    void recalculateRanking_twoCompetitionsInSameTournament_onlyUsesTargetCompetitionMatches() {
        Ranking r11 = new Ranking(11L, 11L, "Nội dung chưa có trận", LocalDateTime.now());
        when(rankingProviderClient.getCompetitionById(11L)).thenReturn(Map.of("id", 11L, "name", "Nội dung 11"));
        when(rankingProviderClient.getCompetitionFormat(11L)).thenReturn(Map.of("competitionId", 11L, "formatType", "ROUND_ROBIN"));
        when(rankingProviderClient.getMatchesByCompetitionId(11L)).thenReturn(List.of());
        when(rankingRepository.findByCompetitionId(11L)).thenReturn(Optional.of(r11));
        when(rankingRepository.save(any(Ranking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rankingItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        RankingResponse res = rankingService.recalculateRanking(11L);
        assertTrue(res.getItems().isEmpty(), "Competition with no matches must yield empty standings instead of falling back to tournament matches");
        verify(rankingItemRepository).deleteByRankingId(11L);
        verify(rankingProviderClient).getMatchesByCompetitionId(11L);
    }

    @Test
    void recalculateRanking_excludesPendingConfirmationEvenWhenMatchStatusIsFinished() {
        Ranking r10 = new Ranking(10L, 10L, "BXH Nội dung 10", LocalDateTime.now());
        when(rankingProviderClient.getCompetitionById(10L)).thenReturn(Map.of("id", 10L, "name", "Nội dung 10"));
        when(rankingProviderClient.getCompetitionFormat(10L)).thenReturn(Map.of("competitionId", 10L, "formatType", "ROUND_ROBIN"));
        when(rankingProviderClient.getMatchesByCompetitionId(10L)).thenReturn(List.of(
                Map.of("id", 100L, "tournamentId", 1L, "competitionId", 10L, "participant1", "Hà Nội", "participant2", "Nam Định", "status", "FINISHED"),
                Map.of("id", 101L, "tournamentId", 1L, "competitionId", 10L, "participant1", "Viettel", "participant2", "CAHN", "status", "FINISHED")
        ));
        // Match 100 result was edited and awaits re-confirmation -> PENDING_CONFIRMATION (must NOT count even though match.status == FINISHED!)
        when(rankingProviderClient.getResultsByMatchIds(List.of(100L, 101L))).thenReturn(List.of(
                Map.of("matchId", 100L, "status", "PENDING_CONFIRMATION", "scoreParticipant1", 5, "scoreParticipant2", 0),
                Map.of("matchId", 101L, "status", "CONFIRMED", "scoreParticipant1", 2, "scoreParticipant2", 1)
        ));
        when(rankingRepository.findByCompetitionId(10L)).thenReturn(Optional.of(r10));
        when(rankingRepository.save(any(Ranking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rankingItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        RankingResponse res = rankingService.recalculateRanking(10L);
        Map<String, RankingItem> byTeam = res.getItems().stream()
                .collect(java.util.stream.Collectors.toMap(RankingItem::getParticipantName, it -> it));

        assertEquals(0, byTeam.get("Hà Nội").getMatchesPlayed(), "Pending confirmation result on FINISHED match must NOT count in standings");
        assertEquals(0, byTeam.get("Hà Nội").getPoints());
        assertEquals(1, byTeam.get("Viettel").getMatchesPlayed());
        assertEquals(3, byTeam.get("Viettel").getPoints());
        assertEquals(1, byTeam.get("CAHN").getMatchesPlayed());
        assertEquals(0, byTeam.get("CAHN").getPoints());
    }

    @Test
    void recalculateRanking_groupsByParticipantIdFirstAndFallsBackToNameForLegacy() {
        Ranking r10 = new Ranking(10L, 10L, "BXH Theo ParticipantId", LocalDateTime.now());
        when(rankingProviderClient.getCompetitionById(10L)).thenReturn(Map.of("id", 10L, "name", "Nội dung 10"));
        when(rankingProviderClient.getCompetitionFormat(10L)).thenReturn(Map.of("competitionId", 10L, "formatType", "ROUND_ROBIN"));
        when(rankingProviderClient.getMatchesByCompetitionId(10L)).thenReturn(List.of(
                Map.of("id", 201L, "tournamentId", 1L, "competitionId", 10L,
                        "participant1Id", 50L, "participant2Id", 51L,
                        "participant1", "Hà Nội FC", "participant2", "Viettel FC", "status", "FINISHED"),
                Map.of("id", 202L, "tournamentId", 1L, "competitionId", 10L,
                        "participant1Id", 50L, "participant2Id", 52L,
                        "participant1", "CLB Hà Nội (Tên mới)", "participant2", "Nam Định", "status", "FINISHED"),
                // Legacy match without participantIds merges by name
                Map.of("id", 203L, "tournamentId", 1L, "competitionId", 10L,
                        "participant1", "Legacy A", "participant2", "Legacy B", "status", "FINISHED")
        ));
        when(rankingProviderClient.getResultsByMatchIds(List.of(201L, 202L, 203L))).thenReturn(List.of(
                Map.of("matchId", 201L, "status", "CONFIRMED", "scoreParticipant1", 2, "scoreParticipant2", 0),
                Map.of("matchId", 202L, "status", "CONFIRMED", "scoreParticipant1", 1, "scoreParticipant2", 1),
                Map.of("matchId", 203L, "status", "CONFIRMED", "scoreParticipant1", 3, "scoreParticipant2", 0)
        ));
        when(rankingRepository.findByCompetitionId(10L)).thenReturn(Optional.of(r10));
        when(rankingRepository.save(any(Ranking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rankingItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        RankingResponse res = rankingService.recalculateRanking(10L);
        RankingItem p50 = res.getItems().stream()
                .filter(it -> Long.valueOf(50L).equals(it.getParticipantId()))
                .findFirst()
                .orElseThrow();
        assertEquals(2, p50.getMatchesPlayed());
        assertEquals(4, p50.getPoints());
        assertEquals(3, p50.getGoalsFor());
        assertEquals(1, p50.getGoalsAgainst());

        RankingItem legacyA = res.getItems().stream()
                .filter(it -> "Legacy A".equals(it.getParticipantName()))
                .findFirst()
                .orElseThrow();
        assertNull(legacyA.getParticipantId());
        assertEquals(3, legacyA.getPoints());
    }

    @Test
    void recalculateRanking_whenUpstreamFails_throws503AndPreservesExistingStandings() {
        when(rankingProviderClient.getCompetitionById(10L)).thenReturn(Map.of("id", 10L, "name", "Nội dung 10"));
        when(rankingProviderClient.getCompetitionFormat(10L)).thenReturn(Map.of("competitionId", 10L, "formatType", "ROUND_ROBIN"));
        when(rankingProviderClient.getMatchesByCompetitionId(10L))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "match-service down"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> rankingService.recalculateRanking(10L));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        verify(rankingItemRepository, never()).deleteByRankingId(any());
        verify(rankingItemRepository, never()).saveAll(any());
        verify(rankingRepository, never()).save(any());
    }

    @Test
    void recalculateRanking_returns404WhenMissing_rejectsKnockout_skipsCancelled_andSortsStableByName() {
        // 1. Missing competition -> ResourceNotFoundException (404) & never saves Ranking
        when(rankingProviderClient.getCompetitionById(404L)).thenReturn(null);
        assertThrows(com.sports.management.exception.ResourceNotFoundException.class,
                () -> rankingService.recalculateRanking(404L));
        verify(rankingRepository, never()).save(any());

        // 2. KNOCKOUT competition -> 400 Bad Request
        when(rankingProviderClient.getCompetitionById(20L)).thenReturn(Map.of("id", 20L, "name", "Cúp Knockout"));
        when(rankingProviderClient.getCompetitionFormat(20L)).thenReturn(Map.of("competitionId", 20L, "formatType", "KNOCKOUT"));
        ResponseStatusException koEx = assertThrows(ResponseStatusException.class,
                () -> rankingService.recalculateRanking(20L));
        assertEquals(HttpStatus.BAD_REQUEST, koEx.getStatusCode());

        // 3. Skips CANCELLED matches & breaks equal points/diff/goals tie alphabetically by participantName
        Ranking r30 = new Ranking(30L, 30L, "BXH Chuẩn", LocalDateTime.now());
        when(rankingProviderClient.getCompetitionById(30L)).thenReturn(Map.of("id", 30L, "name", "Giải Vòng Tròn"));
        when(rankingProviderClient.getCompetitionFormat(30L)).thenReturn(Map.of("competitionId", 30L, "formatType", "ROUND_ROBIN"));
        when(rankingProviderClient.getMatchesByCompetitionId(30L)).thenReturn(List.of(
                Map.of("id", 301L, "competitionId", 30L, "participant1", "Zebra FC", "participant2", "Alpha FC", "status", "FINISHED"),
                Map.of("id", 302L, "competitionId", 30L, "participant1", "Cancelled Team", "participant2", "Alpha FC", "status", "CANCELLED")
        ));
        when(rankingProviderClient.getResultsByMatchIds(List.of(301L))).thenReturn(List.of(
                Map.of("matchId", 301L, "status", "CONFIRMED", "scoreParticipant1", 1, "scoreParticipant2", 1)
        ));
        when(rankingRepository.findByCompetitionId(30L)).thenReturn(Optional.of(r30));
        when(rankingRepository.save(any(Ranking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rankingItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        RankingResponse res = rankingService.recalculateRanking(30L);
        assertEquals(2, res.getItems().size(), "Cancelled team must be ignored cleanly");
        assertEquals("Alpha FC", res.getItems().get(0).getParticipantName());
        assertEquals("Zebra FC", res.getItems().get(1).getParticipantName());
    }

    @Test
    void itemsPagedEndpoint_supportsMultiPageSearchOnSpecificCompetition() throws Exception {
        Ranking r = new Ranking(5L, 10L, "BXH V.League", LocalDateTime.now());
        RankingItem row = new RankingItem(9L, 5L, 6, "CLB Hải Phòng", 4, 2, 1, 1, 7, 6, 4, 2);
        when(rankingRepository.findByCompetitionId(10L)).thenReturn(Optional.of(r));
        PageRequest expectedPage = PageRequest.of(1, 5, Sort.by("rankPosition").ascending());
        when(rankingItemRepository.searchItems(5L, "Hải", expectedPage))
                .thenReturn(new PageImpl<>(List.of(row), expectedPage, 6));

        mockMvc.perform(get("/api/v1/rankings/competition/10/items/paged")
                        .param("keyword", "Hải")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.content[0].participantName").value("CLB Hải Phòng"))
                .andExpect(jsonPath("$.content[0].rankPosition").value(6));
    }
}
