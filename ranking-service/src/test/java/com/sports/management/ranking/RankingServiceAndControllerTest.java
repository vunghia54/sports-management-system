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
        when(rankingProviderClient.getMatchesByCompetitionId(11L)).thenReturn(List.of());
        when(rankingProviderClient.getAllResults()).thenReturn(List.of(
                Map.of("matchId", 101L, "status", "CONFIRMED", "scoreParticipant1", 3, "scoreParticipant2", 1)
        ));
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
        when(rankingProviderClient.getMatchesByCompetitionId(10L)).thenReturn(List.of(
                Map.of("id", 100L, "tournamentId", 1L, "competitionId", 10L, "participant1", "Hà Nội", "participant2", "Nam Định", "status", "FINISHED"),
                Map.of("id", 101L, "tournamentId", 1L, "competitionId", 10L, "participant1", "Viettel", "participant2", "CAHN", "status", "FINISHED")
        ));
        // Match 100 result was edited and awaits re-confirmation -> PENDING_CONFIRMATION (must NOT count even though match.status == FINISHED!)
        when(rankingProviderClient.getAllResults()).thenReturn(List.of(
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
    void recalculateRanking_whenUpstreamFails_throws503AndPreservesExistingStandings() {
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
