package com.sports.management.tournament.service;

import com.sports.management.tournament.model.Competition;
import com.sports.management.tournament.model.CompetitionFormat;
import com.sports.management.tournament.model.CompetitionRule;
import com.sports.management.tournament.model.Sport;
import com.sports.management.tournament.repository.CompetitionFormatRepository;
import com.sports.management.tournament.repository.CompetitionRepository;
import com.sports.management.tournament.repository.CompetitionRuleRepository;
import com.sports.management.tournament.repository.SportRepository;
import com.sports.management.tournament.repository.TournamentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CompetitionService {

    public static final Set<String> ALLOWED_COMPETITION_STATUSES = Set.of(
            "DRAFT", "OPEN_REGISTRATION", "SCHEDULED", "ONGOING", "COMPLETED", "FINISHED", "CANCELLED"
    );

    public static final Set<String> ALLOWED_FORMAT_TYPES = Set.of(
            "ROUND_ROBIN", "KNOCKOUT", "GROUP_STAGE", "HYBRID"
    );

    private final CompetitionRepository competitionRepository;
    private final CompetitionRuleRepository ruleRepository;
    private final CompetitionFormatRepository formatRepository;
    private final SportRepository sportRepository;
    private final TournamentRepository tournamentRepository;

    public CompetitionService(
            CompetitionRepository competitionRepository,
            CompetitionRuleRepository ruleRepository,
            CompetitionFormatRepository formatRepository,
            SportRepository sportRepository) {
        this(competitionRepository, ruleRepository, formatRepository, sportRepository, null);
    }

    @Autowired
    public CompetitionService(
            CompetitionRepository competitionRepository,
            CompetitionRuleRepository ruleRepository,
            CompetitionFormatRepository formatRepository,
            SportRepository sportRepository,
            TournamentRepository tournamentRepository) {
        this.competitionRepository = competitionRepository;
        this.ruleRepository = ruleRepository;
        this.formatRepository = formatRepository;
        this.sportRepository = sportRepository;
        this.tournamentRepository = tournamentRepository;
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
        validateForeignReferencesAndStatus(competition, true);
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
        validateForeignReferencesAndStatus(updated, false);
        existing.setName(updated.getName());
        existing.setSportId(updated.getSportId());
        existing.setTournamentId(updated.getTournamentId());
        if (updated.getStatus() != null && !updated.getStatus().isBlank()) {
            existing.setStatus(updated.getStatus());
        }
        return competitionRepository.save(existing);
    }

    @Transactional
    public boolean deleteCompetition(Long id) {
        if (!competitionRepository.existsById(id)) return false;
        ruleRepository.findByCompetitionId(id).ifPresent(ruleRepository::delete);
        formatRepository.findByCompetitionId(id).ifPresent(formatRepository::delete);
        competitionRepository.deleteById(id);
        return true;
    }

    @Transactional
    public CompetitionRule updateRule(Long competitionId, CompetitionRule updated) {
        if (!competitionRepository.existsById(competitionId)) {
            return null;
        }
        Integer maxParticipants = updated.getMaxParticipants();
        Integer minMembers = updated.getMinTeamMembers();
        Integer maxMembers = updated.getMaxTeamMembers();

        CompetitionRule rule = ruleRepository.findByCompetitionId(competitionId).orElseGet(() -> {
            CompetitionRule created = new CompetitionRule();
            created.setCompetitionId(competitionId);
            return created;
        });

        int resolvedMaxParticipants = maxParticipants != null ? maxParticipants : (rule.getMaxParticipants() != null ? rule.getMaxParticipants() : 16);
        int resolvedMinMembers = minMembers != null ? minMembers : (rule.getMinTeamMembers() != null ? rule.getMinTeamMembers() : 5);
        int resolvedMaxMembers = maxMembers != null ? maxMembers : (rule.getMaxTeamMembers() != null ? rule.getMaxTeamMembers() : 20);

        if (resolvedMaxParticipants <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxParticipants phải lớn hơn 0");
        }
        if (resolvedMinMembers <= 0 || resolvedMaxMembers <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minTeamMembers và maxTeamMembers phải lớn hơn 0");
        }
        if (resolvedMinMembers > resolvedMaxMembers) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "minTeamMembers (" + resolvedMinMembers + ") không được lớn hơn maxTeamMembers (" + resolvedMaxMembers + ")");
        }

        rule.setCompetitionId(competitionId);
        rule.setMaxParticipants(resolvedMaxParticipants);
        rule.setMinTeamMembers(resolvedMinMembers);
        rule.setMaxTeamMembers(resolvedMaxMembers);
        if (updated.getRegistrationDeadline() != null) {
            rule.setRegistrationDeadline(updated.getRegistrationDeadline());
        }
        if (updated.getScoringMethod() != null && !updated.getScoringMethod().isBlank()) {
            rule.setScoringMethod(updated.getScoringMethod().trim().toUpperCase(Locale.ROOT));
        }
        if (updated.getEligibilityCriteria() != null) {
            rule.setEligibilityCriteria(updated.getEligibilityCriteria());
        }
        return ruleRepository.save(rule);
    }

    @Transactional
    public CompetitionFormat updateFormat(Long competitionId, CompetitionFormat updated) {
        if (!competitionRepository.existsById(competitionId)) {
            return null;
        }
        CompetitionFormat format = formatRepository.findByCompetitionId(competitionId).orElseGet(() -> {
            CompetitionFormat created = new CompetitionFormat();
            created.setCompetitionId(competitionId);
            return created;
        });

        if (updated.getFormatType() == null || updated.getFormatType().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "formatType không được để trống");
        }
        String normalizedType = updated.getFormatType().trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_FORMAT_TYPES.contains(normalizedType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "formatType không hợp lệ: " + updated.getFormatType() + ". Cho phép: " + ALLOWED_FORMAT_TYPES);
        }

        int numGroups = updated.getNumberOfGroups() != null ? updated.getNumberOfGroups()
                : (format.getNumberOfGroups() != null ? format.getNumberOfGroups() : 1);
        int qualifyingSpots = updated.getQualifyingSpotsPerGroup() != null ? updated.getQualifyingSpotsPerGroup()
                : (format.getQualifyingSpotsPerGroup() != null ? format.getQualifyingSpotsPerGroup() : 2);
        if (numGroups <= 0 || qualifyingSpots <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "numberOfGroups và qualifyingSpotsPerGroup phải lớn hơn 0");
        }

        format.setCompetitionId(competitionId);
        format.setFormatType(normalizedType);
        format.setNumberOfGroups(numGroups);
        format.setQualifyingSpotsPerGroup(qualifyingSpots);
        if (updated.getHasThirdPlaceMatch() != null) {
            format.setHasThirdPlaceMatch(updated.getHasThirdPlaceMatch());
        }
        return formatRepository.save(format);
    }

    private void validateForeignReferencesAndStatus(Competition comp, boolean defaultStatusIfBlank) {
        if (tournamentRepository != null && (comp.getTournamentId() == null || !tournamentRepository.existsById(comp.getTournamentId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tournament not found with id: " + comp.getTournamentId());
        }
        if (comp.getSportId() == null || !sportRepository.existsById(comp.getSportId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sport not found with id: " + comp.getSportId());
        }
        if (comp.getStatus() == null || comp.getStatus().isBlank()) {
            if (defaultStatusIfBlank) {
                comp.setStatus("OPEN_REGISTRATION");
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái nội dung thi đấu không được để trống");
            }
        } else {
            String normalized = comp.getStatus().trim().toUpperCase(Locale.ROOT);
            if (!ALLOWED_COMPETITION_STATUSES.contains(normalized)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Trạng thái nội dung thi đấu không hợp lệ: " + comp.getStatus() + ". Cho phép: " + ALLOWED_COMPETITION_STATUSES);
            }
            comp.setStatus(normalized);
        }
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

