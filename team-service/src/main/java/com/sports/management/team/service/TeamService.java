package com.sports.management.team.service;

import com.sports.management.team.client.AthleteClient;
import com.sports.management.team.model.Team;
import com.sports.management.team.model.TeamMember;
import com.sports.management.team.repository.TeamMemberRepository;
import com.sports.management.team.repository.TeamRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class TeamService {

    public static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "shortName", "managerName", "createdAt"
    );

    public static final Set<String> ALLOWED_DIRECTIONS = Set.of("asc", "desc");

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
        String resolvedSortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy.trim();
        if (!ALLOWED_SORT_FIELDS.contains(resolvedSortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "sortBy không hợp lệ: '" + sortBy + "'. Cho phép: " + ALLOWED_SORT_FIELDS);
        }
        String resolvedDirection = (direction == null || direction.isBlank()) ? "asc" : direction.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_DIRECTIONS.contains(resolvedDirection)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "direction không hợp lệ: '" + direction + "'. Cho phép: asc, desc");
        }

        Sort sort = "desc".equals(resolvedDirection)
                ? Sort.by(resolvedSortBy).descending()
                : Sort.by(resolvedSortBy).ascending();

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
        if (updates == null) return existing;

        if (updates.containsKey("name")) {
            String nextName = requireStringField(updates.get("name"), "name");
            if (nextName == null || nextName.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team name cannot be blank");
            }
            existing.setName(nextName.trim());
        }
        if (updates.containsKey("shortName")) existing.setShortName(requireStringField(updates.get("shortName"), "shortName"));
        if (updates.containsKey("managerName")) existing.setManagerName(requireStringField(updates.get("managerName"), "managerName"));
        if (updates.containsKey("contactPhone")) existing.setContactPhone(requireStringField(updates.get("contactPhone"), "contactPhone"));
        if (updates.containsKey("contactEmail")) existing.setContactEmail(requireStringField(updates.get("contactEmail"), "contactEmail"));
        if (updates.containsKey("logoUrl")) existing.setLogoUrl(requireStringField(updates.get("logoUrl"), "logoUrl"));
        return teamRepository.save(existing);
    }

    private String requireStringField(Object raw, String fieldName) {
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof String s)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Trường '" + fieldName + "' phải là chuỗi ký tự hợp lệ");
        }
        return s;
    }

    @Transactional
    public boolean deleteTeam(Long id) {
        if (!teamRepository.existsById(id)) return false;
        teamRepository.deleteById(id);
        return true;
    }

    @Transactional
    public TeamMember addMember(TeamMember member) {
        if (member.getTeamId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team ID is required");
        }
        if (member.getAthleteId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Athlete ID is required");
        }
        if (memberRepository.existsByTeamIdAndAthleteId(member.getTeamId(), member.getAthleteId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Vận động viên #" + member.getAthleteId() + " đã có mặt trong đội tuyển #" + member.getTeamId());
        }
        if (member.getJerseyNumber() != null) {
            if (member.getJerseyNumber() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số áo (jerseyNumber) phải lớn hơn 0");
            }
            if (memberRepository.existsByTeamIdAndJerseyNumber(member.getTeamId(), member.getJerseyNumber())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Số áo " + member.getJerseyNumber() + " đã được sử dụng trong đội tuyển #" + member.getTeamId());
            }
        }

        Map<String, Object> athleteInfo = athleteClient.getAthleteById(member.getAthleteId());
        if (athleteInfo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Athlete does not exist with id: " + member.getAthleteId());
        }
        Object healthObj = athleteInfo.get("healthStatus");
        String health = healthObj != null ? String.valueOf(healthObj).trim().toUpperCase(Locale.ROOT) : "";
        if (!"QUALIFIED".equals(health)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vận động viên #" + member.getAthleteId() + " không đủ điều kiện thi đấu (healthStatus=" + health + ", yêu cầu QUALIFIED)");
        }
        if (athleteInfo.get("fullName") != null
                && (member.getAthleteName() == null || member.getAthleteName().isBlank())) {
            member.setAthleteName(String.valueOf(athleteInfo.get("fullName")));
        }

        member.setId(null);
        return memberRepository.save(member);
    }

    public List<TeamMember> getMembersByTeam(Long teamId) {
        return memberRepository.findByTeamId(teamId);
    }

    public boolean isAthleteInAnyTeam(Long athleteId) {
        return athleteId != null && memberRepository.existsByAthleteId(athleteId);
    }

    @Transactional
    public boolean removeMember(Long memberId) {
        if (!memberRepository.existsById(memberId)) return false;
        memberRepository.deleteById(memberId);
        return true;
    }

    @Transactional
    public boolean removeMember(Long teamId, Long memberId) {
        return memberRepository.findById(memberId)
                .filter(member -> Objects.equals(member.getTeamId(), teamId))
                .map(member -> {
                    memberRepository.delete(member);
                    return true;
                })
                .orElse(false);
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalTeams", teamRepository.count());
        stats.put("totalTeamMembers", memberRepository.count());
        return stats;
    }
}
