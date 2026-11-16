package com.sports.management.team.service;

import com.sports.management.team.client.AthleteClient;
import com.sports.management.team.model.Team;
import com.sports.management.team.model.TeamMember;
import com.sports.management.team.repository.TeamMemberRepository;
import com.sports.management.team.repository.TeamRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TeamService {

    public record TeamPage(
            List<Team> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    private final TeamRepository teamRepository;
    private final TeamMemberRepository memberRepository;
    private final AthleteClient athleteClient;

    public TeamService(
            TeamRepository teamRepository,
            TeamMemberRepository memberRepository,
            AthleteClient athleteClient) {
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
        this.athleteClient = athleteClient;
    }

    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    public List<Team> filterTeams(String name, String shortName, String sortBy, String direction) {
        return filterTeams(name, shortName, null, sortBy, direction);
    }

    public List<Team> filterTeams(String name, String shortName, String keyword, String sortBy, String direction) {
        Sort sort = "desc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy != null ? sortBy : "id").descending()
                : Sort.by(sortBy != null ? sortBy : "id").ascending();

        String kw = keyword != null && !keyword.isBlank() ? keyword.trim().toLowerCase() : null;

        return teamRepository.findAll(sort).stream()
                .filter(t -> name == null || name.isBlank()
                        || (t.getName() != null && t.getName().toLowerCase().contains(name.toLowerCase())))
                .filter(t -> shortName == null || shortName.isBlank()
                        || (t.getShortName() != null && t.getShortName().equalsIgnoreCase(shortName)))
                .filter(t -> kw == null
                        || (t.getName() != null && t.getName().toLowerCase().contains(kw))
                        || (t.getShortName() != null && t.getShortName().toLowerCase().contains(kw))
                        || (t.getManagerName() != null && t.getManagerName().toLowerCase().contains(kw))
                        || (t.getContactEmail() != null && t.getContactEmail().toLowerCase().contains(kw)))
                .toList();
    }

    public TeamPage searchTeamsPaged(String name, String shortName, int page, int size, String sortBy, String direction) {
        return searchTeamsPaged(name, shortName, null, page, size, sortBy, direction);
    }

    public TeamPage searchTeamsPaged(String name, String shortName, String keyword, int page, int size, String sortBy, String direction) {
        List<Team> filtered = filterTeams(name, shortName, keyword, sortBy, direction);
        int totalElements = filtered.size();
        int safeSize = Math.max(1, size);
        int safePage = Math.max(0, page);
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);
        int fromIndex = Math.min(safePage * safeSize, totalElements);
        int toIndex = Math.min(fromIndex + safeSize, totalElements);
        return new TeamPage(filtered.subList(fromIndex, toIndex), safePage, safeSize, totalElements, totalPages);
    }

    public Team getTeamById(Long id) {
        return teamRepository.findById(id).orElse(null);
    }

    @Transactional
    public Team createTeam(Team team) {
        team.setId(null);
        return teamRepository.save(team);
    }

    @Transactional
    public Team updateTeam(Long id, Team updated) {
        Team existing = getTeamById(id);
        if (existing == null) return null;
        existing.setName(updated.getName());
        existing.setShortName(updated.getShortName());
        existing.setManagerName(updated.getManagerName());
        existing.setContactPhone(updated.getContactPhone());
        existing.setContactEmail(updated.getContactEmail());
        existing.setLogoUrl(updated.getLogoUrl());
        if (updated.getOwnerUsername() != null) {
            existing.setOwnerUsername(updated.getOwnerUsername());
        }
        return teamRepository.save(existing);
    }

    @Transactional
    public Team patchTeam(Long id, Map<String, Object> updates) {
        Team existing = getTeamById(id);
        if (existing == null) return null;
        if (updates.containsKey("name")) existing.setName((String) updates.get("name"));
        if (updates.containsKey("shortName")) existing.setShortName((String) updates.get("shortName"));
        if (updates.containsKey("managerName")) existing.setManagerName((String) updates.get("managerName"));
        if (updates.containsKey("contactPhone")) existing.setContactPhone((String) updates.get("contactPhone"));
        if (updates.containsKey("contactEmail")) existing.setContactEmail((String) updates.get("contactEmail"));
        if (updates.containsKey("logoUrl")) existing.setLogoUrl((String) updates.get("logoUrl"));
        return teamRepository.save(existing);
    }

    @Transactional
    public boolean deleteTeam(Long id) {
        if (!teamRepository.existsById(id)) return false;
        teamRepository.deleteById(id);
        return true;
    }

    @Transactional
    public TeamMember addMember(TeamMember member) {
        if (member.getAthleteId() != null) {
            Map<String, Object> athleteInfo = athleteClient.getAthleteById(member.getAthleteId());
            if (athleteInfo != null && athleteInfo.get("fullName") != null
                    && (member.getAthleteName() == null || member.getAthleteName().isBlank())) {
                member.setAthleteName(String.valueOf(athleteInfo.get("fullName")));
            }
        }
        member.setId(null);
        return memberRepository.save(member);
    }

    public List<TeamMember> getMembersByTeam(Long teamId) {
        return memberRepository.findByTeamId(teamId);
    }

    @Transactional
    public boolean removeMember(Long memberId) {
        if (!memberRepository.existsById(memberId)) return false;
        memberRepository.deleteById(memberId);
        return true;
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalTeams", teamRepository.count());
        stats.put("totalTeamMembers", memberRepository.count());
        return stats;
    }
}
