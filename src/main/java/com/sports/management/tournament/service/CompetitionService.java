package com.sports.management.tournament.service;

import com.sports.management.tournament.model.Competition;
import com.sports.management.tournament.model.CompetitionFormat;
import com.sports.management.tournament.model.CompetitionRule;
import com.sports.management.tournament.model.Sport;
import com.sports.management.tournament.repository.CompetitionFormatRepository;
import com.sports.management.tournament.repository.CompetitionRepository;
import com.sports.management.tournament.repository.CompetitionRuleRepository;
import com.sports.management.tournament.repository.SportRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionRuleRepository ruleRepository;
    private final CompetitionFormatRepository formatRepository;
    private final SportRepository sportRepository;

    public CompetitionService(
            CompetitionRepository competitionRepository,
            CompetitionRuleRepository ruleRepository,
            CompetitionFormatRepository formatRepository,
            SportRepository sportRepository) {
        this.competitionRepository = competitionRepository;
        this.ruleRepository = ruleRepository;
        this.formatRepository = formatRepository;
        this.sportRepository = sportRepository;
    }

    public List<Competition> getAllCompetitions() {
        return competitionRepository.findAll();
    }

    public List<Competition> getCompetitionsByTournament(Long tournamentId) {
        return competitionRepository.findByTournamentId(tournamentId);
    }

    public Competition getCompetitionById(Long id) {
        return competitionRepository.findById(id).orElse(null);
    }

    public Competition createCompetition(Competition competition) {
        competition.setId(null);
        Competition saved = competitionRepository.save(competition);

        // Tạo rule & format mặc định nếu chưa có
        if (ruleRepository.findByCompetitionId(saved.getId()).isEmpty()) {
            CompetitionRule rule = new CompetitionRule();
            rule.setCompetitionId(saved.getId());
            ruleRepository.save(rule);
        }

        if (formatRepository.findByCompetitionId(saved.getId()).isEmpty()) {
            CompetitionFormat format = new CompetitionFormat();
            format.setCompetitionId(saved.getId());
            formatRepository.save(format);
        }

        return saved;
    }

    public CompetitionRule getRuleByCompetition(Long competitionId) {
        return ruleRepository.findByCompetitionId(competitionId).orElse(null);
    }

    public CompetitionFormat getFormatByCompetition(Long competitionId) {
        return formatRepository.findByCompetitionId(competitionId).orElse(null);
    }

    public List<Sport> getAllSports() {
        return sportRepository.findAll();
    }

    public Sport createSport(Sport sport) {
        return sportRepository.save(sport);
    }
}
