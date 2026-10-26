package com.sports.management.team.service;

import com.sports.management.team.model.Team;
import com.sports.management.team.model.TeamMember;
import com.sports.management.team.repository.TeamMemberRepository;
import com.sports.management.team.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository memberRepository;

    public TeamService(TeamRepository teamRepository, TeamMemberRepository memberRepository) {
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
    }

    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    public Team getTeamById(Long id) {
        return teamRepository.findById(id).orElse(null);
    }

    public Team createTeam(Team team) {
        team.setId(null);
        return teamRepository.save(team);
    }

    public TeamMember addMember(TeamMember member) {
        member.setId(null);
        return memberRepository.save(member);
    }

    public List<TeamMember> getMembersByTeam(Long teamId) {
        return memberRepository.findByTeamId(teamId);
    }
}
