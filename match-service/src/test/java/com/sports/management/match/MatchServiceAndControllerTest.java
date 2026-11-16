package com.sports.management.match;

import com.sports.management.exception.GlobalExceptionHandler;
import com.sports.management.match.client.TournamentClient;
import com.sports.management.match.controller.MatchController;
import com.sports.management.match.model.Match;
import com.sports.management.match.repository.MatchRepository;
import com.sports.management.match.repository.ParticipantRepository;
import com.sports.management.match.service.MatchService;
import com.sports.management.messaging.producer.MatchEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MatchServiceAndControllerTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private TournamentClient tournamentClient;

    @Mock
    private MatchEventProducer matchEventProducer;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MatchService matchService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        matchService = new MatchService(
                matchRepository,
                participantRepository,
                tournamentClient,
                matchEventProducer,
                eventPublisher
        );
        MatchController controller = new MatchController(matchService, tournamentClient, "match-service:8083");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void checkScheduleConflicts_detectsVenueRefereeTeamAndRespectsExcludeId() {
        LocalDateTime time = LocalDateTime.of(2026, 11, 20, 19, 0);
        Match existing = new Match(10L, 1L, "Vòng 1", time, "SVĐ Mỹ Đình", "Hà Nội", "Nam Định", "SCHEDULED");
        existing.setReferee("Ngô Duy Lân");

        Match cancelled = new Match(11L, 1L, "Vòng 1", time, "SVĐ Mỹ Đình", "A", "B", "CANCELLED");
        cancelled.setReferee("Ngô Duy Lân");

        when(matchRepository.findAll()).thenReturn(List.of(existing, cancelled));

        Match candidate = new Match(null, 1L, "Vòng 2", time.plusMinutes(30), "SVĐ Mỹ Đình", "Hà Nội", "Viettel", "SCHEDULED");
        candidate.setReferee("Ngô Duy Lân");

        List<Map<String, Object>> conflicts = matchService.checkScheduleConflicts(candidate, null);
        assertEquals(3, conflicts.size());
        assertTrue(conflicts.stream().anyMatch(c -> "VENUE_CONFLICT".equals(c.get("type"))));
        assertTrue(conflicts.stream().anyMatch(c -> "REFEREE_CONFLICT".equals(c.get("type"))));
        assertTrue(conflicts.stream().anyMatch(c -> "TEAM_CONFLICT".equals(c.get("type"))));

        // When editing the exact same match id=10, it should not conflict with itself
        List<Map<String, Object>> selfEditConflicts = matchService.checkScheduleConflicts(candidate, 10L);
        assertTrue(selfEditConflicts.isEmpty());
    }

    @Test
    void checkScheduleConflicts_detectsSameParticipant() {
        Match candidate = new Match();
        candidate.setParticipant1("CLB Hà Nội");
        candidate.setParticipant2("clb hà nội");
        List<Map<String, Object>> conflicts = matchService.checkScheduleConflicts(candidate, null);
        assertEquals(1, conflicts.size());
        assertEquals("SAME_PARTICIPANT", conflicts.get(0).get("type"));
    }

    @Test
    void searchMatches_normalizesBlankStatusAndKeywordToNull() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Match> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(matchRepository.searchMatches(eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(emptyPage);

        Page<Match> res = matchService.searchMatches(1L, null, "   ", "  ", null, pageable);
        assertEquals(0, res.getTotalElements());
        verify(matchRepository).searchMatches(1L, null, null, null, null, null, pageable);
    }

    @Test
    void searchMatches_withDateComputesExactDayWindow() {
        PageRequest pageable = PageRequest.of(2, 5);
        LocalDate day = LocalDate.of(2026, 12, 10);
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();
        Page<Match> page = new PageImpl<>(List.of(), pageable, 0);
        when(matchRepository.searchMatches(isNull(), isNull(), eq("SCHEDULED"), eq("Hà Nội"), eq(start), eq(end), eq(pageable)))
                .thenReturn(page);

        Page<Match> res = matchService.searchMatches(null, null, "SCHEDULED", "Hà Nội", day, pageable);
        assertEquals(0, res.getTotalElements());
        verify(matchRepository).searchMatches(null, null, "SCHEDULED", "Hà Nội", start, end, pageable);
    }

    @Test
    void createMatch_rejectsIdenticalParticipants() throws Exception {
        mockMvc.perform(post("/api/v1/matches")
                        .principal(new TestingAuthenticationToken("admin", null, "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tournamentId\":1,\"competitionId\":10,\"participant1Id\":1,\"participant2Id\":1,\"matchTime\":\"2026-12-15T18:00:00\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteMatch_returnsConflictWhenFinished() throws Exception {
        Match finished = new Match(5L, 1L, "Chung kết", LocalDateTime.now(), "SVĐ A", "Đội 1", "Đội 2", "FINISHED");
        when(matchRepository.findById(5L)).thenReturn(Optional.of(finished));

        mockMvc.perform(delete("/api/v1/matches/5")
                        .principal(new TestingAuthenticationToken("admin", null, "ROLE_ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
