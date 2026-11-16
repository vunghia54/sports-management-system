package com.sports.management.tournament;

import com.sports.management.exception.GlobalExceptionHandler;
import com.sports.management.tournament.controller.TournamentController;
import com.sports.management.tournament.model.Competition;
import com.sports.management.tournament.model.Tournament;
import com.sports.management.tournament.repository.TournamentRepository;
import com.sports.management.tournament.service.CompetitionService;
import com.sports.management.tournament.service.TournamentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TournamentValidationAndDeleteTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private CompetitionService competitionService;

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    private TournamentService tournamentService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        tournamentService = new TournamentService(tournamentRepository);
        when(restClientBuilder.build()).thenReturn(restClient);
        TournamentController controller = new TournamentController(tournamentService, competitionService, restClientBuilder);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Tournament validTournament() {
        Tournament t = new Tournament();
        t.setName("V-League 2026");
        t.setLocation("Hà Nội");
        t.setStartDate(LocalDate.of(2026, 10, 15));
        t.setEndDate(LocalDate.of(2026, 12, 20));
        t.setRegistrationStartDate(LocalDate.of(2026, 9, 1));
        t.setRegistrationEndDate(LocalDate.of(2026, 10, 10));
        t.setStatus("UPCOMING");
        t.setOrganizerUsername("organizer1");
        return t;
    }

    @Test
    void createTournament_rejectsStartDateAfterEndDate() {
        Tournament t = validTournament();
        t.setStartDate(LocalDate.of(2026, 12, 25));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> tournamentService.createTournament(t));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void createTournament_rejectsRegistrationEndAfterStartDate() {
        Tournament t = validTournament();
        t.setRegistrationEndDate(LocalDate.of(2026, 10, 20));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> tournamentService.createTournament(t));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchTournaments_returnsPagedResultsAndRejectsInvalidSort() {
        Tournament t = validTournament();
        t.setId(1L);
        Page<Tournament> page = new PageImpl<>(List.of(t));
        when(tournamentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        TournamentService.TournamentPage result = tournamentService.searchTournaments(
                null, "V-League", "UPCOMING", 0, 10, "startDate", "asc");
        assertEquals(1, result.totalElements());
        assertEquals("V-League 2026", result.content().get(0).getName());

        ResponseStatusException badSort = assertThrows(ResponseStatusException.class,
                () -> tournamentService.searchTournaments(null, null, null, 0, 10, "hackField", "asc"));
        assertEquals(HttpStatus.BAD_REQUEST, badSort.getStatusCode());
    }

    @Test
    void deleteTournament_returnsConflictWhenLinkedCompetitionsExist() throws Exception {
        Tournament t = validTournament();
        t.setId(1L);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(t));
        when(competitionService.getCompetitionsByTournament(1L))
                .thenReturn(List.of(new Competition(10L, 1L, 1L, "Bóng đá Nam", "OPEN_REGISTRATION")));

        var adminAuth = new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        mockMvc.perform(delete("/api/v1/tournaments/1").principal(adminAuth))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void deleteTournament_returnsForbiddenForNonOwnerOrganizer() throws Exception {
        Tournament t = validTournament();
        t.setId(1L);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(t));

        var otherOrg = new UsernamePasswordAuthenticationToken(
                "organizer2", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ORGANIZER")));

        mockMvc.perform(delete("/api/v1/tournaments/1").principal(otherOrg))
                .andExpect(status().isForbidden());
    }
}
