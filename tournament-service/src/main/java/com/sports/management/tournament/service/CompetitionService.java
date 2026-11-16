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
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public Competition createCompetition(Competition competition) {
        competition.setId(null);
        Competition saved = competitionRepository.save(competition);

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

    @Transactional
    public Competition updateCompetition(Long id, Competition updated) {
        Competition existing = getCompetitionById(id);
        if (existing == null) return null;
        existing.setName(updated.getName());
        existing.setSportId(updated.getSportId());
        existing.setTournamentId(updated.getTournamentId());
        if (updated.getStatus() != null) {
            existing.setStatus(updated.getStatus());
        }
        return competitionRepository.save(existing);
    }

    @Transactional
    public boolean deleteCompetition(Long id) {
        if (!competitionRepository.existsById(id)) return false;
        competitionRepository.deleteById(id);
        return true;
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

    public Sport getSportById(Long id) {
        return sportRepository.findById(id).orElse(null);
    }

    @Transactional
    public Sport createSport(Sport sport) {
        sport.setId(null);
        return sportRepository.save(sport);
    }

    @Transactional
    public Sport updateSport(Long id, Sport updated) {
        Sport existing = getSportById(id);
        if (existing == null) return null;
        existing.setName(updated.getName());
        existing.setSportType(updated.getSportType());
        existing.setDescription(updated.getDescription());
        return sportRepository.save(existing);
    }

    @Transactional
    public boolean deleteSport(Long id) {
        if (!sportRepository.existsById(id)) return false;
        List<Competition> linked = competitionRepository.findBySportId(id);
        if (linked != null && !linked.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Không thể xóa môn thể thao vì đang có " + linked.size() + " nội dung thi đấu liên kết."
            );
        }
        sportRepository.deleteById(id);
        return true;
    }
}

