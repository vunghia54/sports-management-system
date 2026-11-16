package com.sports.management.match.service;

import com.sports.management.exception.ResourceNotFoundException;
import com.sports.management.match.client.TournamentClient;
import com.sports.management.match.model.Participant;
import com.sports.management.match.model.Registration;
import com.sports.management.match.repository.ParticipantRepository;
import com.sports.management.match.repository.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final ParticipantRepository participantRepository;
    private final TournamentClient tournamentClient;

    public RegistrationService(
            RegistrationRepository registrationRepository,
            ParticipantRepository participantRepository,
            TournamentClient tournamentClient) {
        this.registrationRepository = registrationRepository;
        this.participantRepository = participantRepository;
        this.tournamentClient = tournamentClient;
    }

    public record RegistrationPage(
            List<Registration> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    public Map<String, Object> getStatistics() {
        List<Registration> all = registrationRepository.findAll();
        long pending = all.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getStatus())).count();
        long approved = all.stream().filter(r -> "APPROVED".equalsIgnoreCase(r.getStatus())).count();
        long rejected = all.stream().filter(r -> "REJECTED".equalsIgnoreCase(r.getStatus())).count();
        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("totalRegistrations", all.size());
        stats.put("pendingRegistrations", pending);
        stats.put("approvedRegistrations", approved);
        stats.put("rejectedRegistrations", rejected);
        return stats;
    }

    public List<Registration> getAllRegistrations() {
        return registrationRepository.findAll();
    }

    public List<Registration> filterRegistrations(Long competitionId, String status, String keyword) {
        return filterRegistrations(null, competitionId, status, keyword);
    }

    public List<Registration> filterRegistrations(Long tournamentId, Long competitionId, String status, String keyword) {
        String safeStatus = (status != null && !status.isBlank()) ? status.trim() : null;
        String safeKw = (keyword != null && !keyword.isBlank()) ? keyword.trim().toLowerCase() : null;
        java.util.Set<Long> tournamentCompIds = null;
        if (tournamentId != null && competitionId == null) {
            tournamentCompIds = tournamentClient.getCompetitionsByTournament(tournamentId).stream()
                    .filter(c -> c.get("id") != null)
                    .map(c -> Long.valueOf(String.valueOf(c.get("id"))))
                    .collect(java.util.stream.Collectors.toSet());
        }
        final java.util.Set<Long> allowedComps = tournamentCompIds;
        return registrationRepository.findAll().stream()
                .filter(r -> allowedComps == null || allowedComps.contains(r.getCompetitionId()))
                .filter(r -> competitionId == null || competitionId.equals(r.getCompetitionId()))
                .filter(r -> safeStatus == null || (r.getStatus() != null && r.getStatus().equalsIgnoreCase(safeStatus)))
                .filter(r -> safeKw == null
                        || (r.getParticipantName() != null && r.getParticipantName().toLowerCase().contains(safeKw))
                        || (r.getParticipantType() != null && r.getParticipantType().toLowerCase().contains(safeKw))
                        || (r.getRejectionReason() != null && r.getRejectionReason().toLowerCase().contains(safeKw)))
                .toList();
    }

    public RegistrationPage searchRegistrationsPaged(Long competitionId, String status, String keyword, int page, int size) {
        return searchRegistrationsPaged(null, competitionId, status, keyword, page, size);
    }

    public RegistrationPage searchRegistrationsPaged(Long tournamentId, Long competitionId, String status, String keyword, int page, int size) {
        List<Registration> filtered = filterRegistrations(tournamentId, competitionId, status, keyword);
        int totalElements = filtered.size();
        int safeSize = Math.max(1, size);
        int safePage = Math.max(0, page);
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);
        int fromIndex = Math.min(safePage * safeSize, totalElements);
        int toIndex = Math.min(fromIndex + safeSize, totalElements);
        return new RegistrationPage(filtered.subList(fromIndex, toIndex), safePage, safeSize, totalElements, totalPages);
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
        // 1. Kiem tra competition ton tai qua Eureka Service Discovery (TOURNAMENT-SERVICE)
        tournamentClient.getCompetitionById(registration.getCompetitionId());

        // 2. Kiem tra doi tuong dang ky (TEAM-SERVICE hoac ATHLETE-SERVICE) qua Eureka Service Discovery
        String type = registration.getParticipantType().toUpperCase();
        if ("TEAM".equals(type)) {
            Map<String, Object> team = tournamentClient.getTeamById(registration.getParticipantReferenceId());
            if ((registration.getParticipantName() == null || registration.getParticipantName().isBlank())
                    && team != null && team.get("teamName") != null) {
                registration.setParticipantName(String.valueOf(team.get("teamName")));
            }
        } else if ("ATHLETE".equals(type)) {
            Map<String, Object> athlete = tournamentClient.getAthleteById(registration.getParticipantReferenceId());
            if ((registration.getParticipantName() == null || registration.getParticipantName().isBlank())
                    && athlete != null && athlete.get("fullName") != null) {
                registration.setParticipantName(String.valueOf(athlete.get("fullName")));
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

