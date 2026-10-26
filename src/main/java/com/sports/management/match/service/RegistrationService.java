package com.sports.management.match.service;

import com.sports.management.athlete.model.Athlete;
import com.sports.management.athlete.service.AthleteService;
import com.sports.management.exception.ResourceNotFoundException;
import com.sports.management.match.model.Participant;
import com.sports.management.match.model.Registration;
import com.sports.management.match.repository.ParticipantRepository;
import com.sports.management.match.repository.RegistrationRepository;
import com.sports.management.team.model.Team;
import com.sports.management.team.service.TeamService;
import com.sports.management.tournament.service.CompetitionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final ParticipantRepository participantRepository;
    private final CompetitionService competitionService;
    private final TeamService teamService;
    private final AthleteService athleteService;

    public RegistrationService(
            RegistrationRepository registrationRepository,
            ParticipantRepository participantRepository,
            CompetitionService competitionService,
            TeamService teamService,
            AthleteService athleteService) {
        this.registrationRepository = registrationRepository;
        this.participantRepository = participantRepository;
        this.competitionService = competitionService;
        this.teamService = teamService;
        this.athleteService = athleteService;
    }

    public List<Registration> getAllRegistrations() {
        return registrationRepository.findAll();
    }

    public Registration getRegistrationById(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with id: " + id));
    }

    public List<Registration> getRegistrationsByCompetition(Long competitionId) {
        return registrationRepository.findByCompetitionId(competitionId);
    }

    public List<Participant> getParticipantsByCompetition(Long competitionId) {
        return participantRepository.findByCompetitionId(competitionId);
    }

    @Transactional
    public Registration register(Registration registration) {
        // 1. Kiem tra competition ton tai
        competitionService.getCompetitionById(registration.getCompetitionId());

        // 2. Kiem tra doi tuong dang ky (Team hoac Athlete)
        String type = registration.getParticipantType().toUpperCase();
        if ("TEAM".equals(type)) {
            Team team = teamService.getTeamById(registration.getParticipantReferenceId());
            if (registration.getParticipantName() == null || registration.getParticipantName().isBlank()) {
                registration.setParticipantName(team.getTeamName());
            }
        } else if ("ATHLETE".equals(type)) {
            Athlete athlete = athleteService.getAthleteById(registration.getParticipantReferenceId());
            if (registration.getParticipantName() == null || registration.getParticipantName().isBlank()) {
                registration.setParticipantName(athlete.getFullName());
            }
        } else {
            throw new IllegalArgumentException("Unsupported participant type: " + type + ". Must be TEAM or ATHLETE.");
        }

        // 3. Kiem tra trung lap dang ky
        registrationRepository.findByCompetitionIdAndParticipantTypeAndParticipantReferenceId(
                registration.getCompetitionId(), type, registration.getParticipantReferenceId()
        ).ifPresent(r -> {
            throw new IllegalStateException("Participant is already registered for this competition with status: " + r.getStatus());
        });

        registration.setId(null);
        registration.setParticipantType(type);
        registration.setStatus("PENDING");
        if (registration.getRegistrationDate() == null) {
            registration.setRegistrationDate(LocalDateTime.now());
        }

        return registrationRepository.save(registration);
    }

    @Transactional
    public Registration approveRegistration(Long id) {
        Registration reg = getRegistrationById(id);
        reg.setStatus("APPROVED");
        reg.setRejectionReason(null);
        Registration savedReg = registrationRepository.save(reg);

        // Tu dong tao Participant cho noi dung thi dau
        participantRepository.findByCompetitionIdAndParticipantTypeAndReferenceId(
                reg.getCompetitionId(), reg.getParticipantType(), reg.getParticipantReferenceId()
        ).orElseGet(() -> {
            Participant participant = new Participant(
                    null,
                    reg.getCompetitionId(),
                    reg.getId(),
                    reg.getParticipantType(),
                    reg.getParticipantReferenceId(),
                    reg.getParticipantName(),
                    null
            );
            return participantRepository.save(participant);
        });

        return savedReg;
    }

    @Transactional
    public Registration rejectRegistration(Long id, String reason) {
        Registration reg = getRegistrationById(id);
        reg.setStatus("REJECTED");
        reg.setRejectionReason(reason);
        return registrationRepository.save(reg);
    }
}
