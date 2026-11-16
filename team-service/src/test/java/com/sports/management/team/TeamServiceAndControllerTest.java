package com.sports.management.team;

import com.sports.management.exception.GlobalExceptionHandler;
import com.sports.management.team.client.AthleteClient;
import com.sports.management.team.controller.TeamController;
import com.sports.management.team.model.Team;
import com.sports.management.team.model.TeamMember;
import com.sports.management.team.repository.TeamMemberRepository;
import com.sports.management.team.repository.TeamRepository;
import com.sports.management.team.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TeamServiceAndControllerTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository memberRepository;

    @Mock
    private AthleteClient athleteClient;

    private TeamService teamService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        teamService = new TeamService(teamRepository, memberRepository, athleteClient);
        TeamController controller = new TeamController(teamService, athleteClient);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void searchTeamsPaged_filtersByKeywordAcrossAllDataAndPaginates() {
        Team t1 = new Team(1L, "CLB Bóng Đá Hà Nội", "HNFC", "Nguyễn Văn A", "0901", "hn@fc.vn", null, LocalDateTime.now());
        Team t2 = new Team(2L, "CLB Thể Công Viettel", "TCVT", "Trần Văn B", "0902", "tc@fc.vn", null, LocalDateTime.now());
        Team t3 = new Team(3L, "CLB Công An Hà Nội", "CAHN", "Lê Văn C", "0903", "cahn@fc.vn", null, LocalDateTime.now());
        when(teamRepository.findAll(any(Sort.class))).thenReturn(List.of(t1, t2, t3));

        TeamService.TeamPage page0 = teamService.searchTeamsPaged(null, null, "Hà Nội", 0, 1, "id", "asc");
        assertEquals(2, page0.totalElements());
        assertEquals(2, page0.totalPages());
        assertEquals(1, page0.content().size());
        assertEquals("CLB Bóng Đá Hà Nội", page0.content().get(0).getName());

        TeamService.TeamPage page1 = teamService.searchTeamsPaged(null, null, "Hà Nội", 1, 1, "id", "asc");
        assertEquals("CLB Công An Hà Nội", page1.content().get(0).getName());
    }

    @Test
    void deleteTeam_returnsConflictWhenMembersExist() throws Exception {
        Team t = new Team(1L, "CLB Hà Nội", "HNFC", "HLV A", "0901", "hn@fc.vn", null, LocalDateTime.now());
        t.setOwnerUsername("manager1");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(t));
        when(memberRepository.findByTeamId(1L)).thenReturn(List.of(new TeamMember()));

        var adminAuth = new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        mockMvc.perform(delete("/api/v1/teams/1").principal(adminAuth))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void deleteTeam_succeedsWhenEmpty() throws Exception {
        Team t = new Team(2L, "CLB Trống", "EMPTY", "HLV B", "0902", "b@fc.vn", null, LocalDateTime.now());
        when(teamRepository.findById(2L)).thenReturn(Optional.of(t));
        when(teamRepository.existsById(2L)).thenReturn(true);
        when(memberRepository.findByTeamId(2L)).thenReturn(Collections.emptyList());

        var adminAuth = new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        mockMvc.perform(delete("/api/v1/teams/2").principal(adminAuth))
                .andExpect(status().isNoContent());
        verify(teamRepository).deleteById(2L);
    }

    @Test
    void addMember_forbidsNonOwnerManager() throws Exception {
        Team t = new Team(1L, "CLB Hà Nội", "HNFC", "HLV A", "0901", "hn@fc.vn", null, LocalDateTime.now());
        t.setOwnerUsername("manager1");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(t));

        var otherManager = new UsernamePasswordAuthenticationToken(
                "manager2", "n/a", List.of(new SimpleGrantedAuthority("ROLE_TEAM_MANAGER")));

        mockMvc.perform(post("/api/v1/teams/1/members")
                        .principal(otherManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"athleteName\":\"Nguyễn Văn X\",\"jerseyNumber\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void removeMember_allowsOwnerManagerAndForbidsNonOwner() throws Exception {
        Team t = new Team(1L, "CLB Hà Nội", "HNFC", "HLV A", "0901", "hn@fc.vn", null, LocalDateTime.now());
        t.setOwnerUsername("manager1");
        TeamMember member = new TeamMember();
        member.setId(77L);
        member.setTeamId(1L);
        member.setAthleteId(10L);
        when(teamRepository.findById(1L)).thenReturn(Optional.of(t));
        when(memberRepository.findById(77L)).thenReturn(Optional.of(member));

        var ownerManager = new UsernamePasswordAuthenticationToken(
                "manager1", "n/a", List.of(new SimpleGrantedAuthority("ROLE_TEAM_MANAGER")));
        mockMvc.perform(delete("/api/v1/teams/1/members/77").principal(ownerManager))
                .andExpect(status().isNoContent());

        var otherManager = new UsernamePasswordAuthenticationToken(
                "manager2", "n/a", List.of(new SimpleGrantedAuthority("ROLE_TEAM_MANAGER")));
        mockMvc.perform(delete("/api/v1/teams/1/members/77").principal(otherManager))
                .andExpect(status().isForbidden());
    }
}
