package com.sports.management.match.service;

import com.sports.management.exception.ResourceNotFoundException;
import com.sports.management.match.client.TournamentClient;
import com.sports.management.match.model.Match;
import com.sports.management.match.model.Participant;
import com.sports.management.match.model.Registration;
import com.sports.management.match.repository.MatchRepository;
import com.sports.management.match.repository.ParticipantRepository;
import com.sports.management.match.repository.RegistrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final ParticipantRepository participantRepository;
    private final TournamentClient tournamentClient;
    private final MatchRepository matchRepository;

    public RegistrationService(
            RegistrationRepository registrationRepository,
            ParticipantRepository participantRepository,
            TournamentClient tournamentClient) {
        this(registrationRepository, participantRepository, tournamentClient, null);
    }

    @Autowired
    public RegistrationService(
            RegistrationRepository registrationRepository,
            ParticipantRepository participantRepository,
            TournamentClient tournamentClient,
            MatchRepository matchRepository) {
        this.registrationRepository = registrationRepository;
        this.participantRepository = participantRepository;
        this.tournamentClient = tournamentClient;
        this.matchRepository = matchRepository;
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
        return register(registration, registration.getRegisteredBy(), true);
    }

    @Transactional
    public Registration register(Registration registration, String callerUsername, boolean isAdmin) {
        if (callerUsername == null || callerUsername.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Full authentication is required to register");
        }

        // 1. Verify competition exists & status == OPEN_REGISTRATION
        Map<String, Object> comp = tournamentClient.getCompetitionById(registration.getCompetitionId());
        if (comp == null) {
            throw new ResourceNotFoundException("Competition not found with id: " + registration.getCompetitionId());
        }
        String compStatus = comp.get("status") != null ? String.valueOf(comp.get("status")).trim() : "";
        if (!"OPEN_REGISTRATION".equalsIgnoreCase(compStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nội dung thi đấu hiện không mở đăng ký (trạng thái: " + compStatus + ")");
        }
        if (matchRepository != null && !matchRepository.findByCompetitionId(registration.getCompetitionId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Nội dung thi đấu #" + registration.getCompetitionId() + " đã có lịch thi đấu, không thể đăng ký thêm");
        }

        // 2. Verify registration window [registrationStartDate, registrationEndDate] of tournament and rule registrationDeadline
        Long tournamentId = parseLongField(comp.get("tournamentId"));
        Map<String, Object> tour = tournamentId != null ? tournamentClient.getTournamentById(tournamentId) : null;
        if (tour == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy thông tin giải đấu cho nội dung #" + registration.getCompetitionId());
        }
        LocalDate regStart = parseLocalDate(tour.get("registrationStartDate"));
        LocalDate regEnd = parseLocalDate(tour.get("registrationEndDate"));
        LocalDate today = LocalDate.now();
        if ((regStart != null && today.isBefore(regStart)) || (regEnd != null && today.isAfter(regEnd))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ngày hiện tại (" + today + ") nằm ngoài thời hạn đăng ký [" + regStart + ", " + regEnd + "]");
        }
        Map<String, Object> rule = tournamentClient.getCompetitionRule(registration.getCompetitionId());
        if (rule != null && rule.get("registrationDeadline") != null) {
            LocalDateTime deadline = parseLocalDateTime(rule.get("registrationDeadline"));
            if (deadline != null && LocalDateTime.now().isAfter(deadline)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Đã quá hạn chót đăng ký của nội dung thi đấu (" + deadline + ")");
            }
        }

        // 3. Verify participantType matches Sport.sportType
        String type = registration.getParticipantType() != null
                ? registration.getParticipantType().trim().toUpperCase(Locale.ROOT)
                : "";
        if (!"TEAM".equals(type) && !"ATHLETE".equals(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Loại đối tượng đăng ký không hợp lệ: " + type + ". Chỉ chấp nhận TEAM hoặc ATHLETE");
        }
        Long sportId = parseLongField(comp.get("sportId"));
        if (sportId != null) {
            Map<String, Object> sport = tournamentClient.getSportById(sportId);
            String sportType = sport != null && sport.get("sportType") != null
                    ? String.valueOf(sport.get("sportType")).trim().toUpperCase(Locale.ROOT)
                    : "";
            if ("TEAM".equals(sportType) && !"TEAM".equals(type)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Môn thể thao đồng đội chỉ chấp nhận đăng ký theo Đội (TEAM)");
            }
            if ("INDIVIDUAL".equals(sportType) && !"ATHLETE".equals(type)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Môn thể thao cá nhân chỉ chấp nhận đăng ký theo Vận động viên (ATHLETE)");
            }
        }

        // 4. Verify participant (Team / Athlete), ownership, rule bounds & athlete health + no duplicate athlete across active teams
        if ("TEAM".equals(type)) {
            Map<String, Object> team = tournamentClient.getTeamById(registration.getParticipantReferenceId());
            if (team == null) {
                throw new ResourceNotFoundException("Team not found with id: " + registration.getParticipantReferenceId());
            }
            if (!isAdmin) {
                String owner = team.get("ownerUsername") != null ? String.valueOf(team.get("ownerUsername")) : null;
                if (owner == null || owner.isBlank() || !owner.equalsIgnoreCase(callerUsername)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Access Denied: Bạn chỉ được phép đăng ký đội tuyển thuộc quyền sở hữu của mình");
                }
            }
            Object rawName = team.get("name") != null ? team.get("name") : team.get("teamName");
            if (rawName != null && !String.valueOf(rawName).isBlank()) {
                registration.setParticipantName(String.valueOf(rawName));
            }
            List<Map<String, Object>> members = tournamentClient.getTeamMembers(registration.getParticipantReferenceId());
            int memberCount = members != null ? members.size() : 0;
            Integer minMembers = rule != null ? parseIntField(rule.get("minTeamMembers")) : null;
            Integer maxMembers = rule != null ? parseIntField(rule.get("maxTeamMembers")) : null;
            if (minMembers != null && memberCount < minMembers) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Đội '" + registration.getParticipantName() + "' mới có " + memberCount
                                + " thành viên, chưa đạt tối thiểu " + minMembers + " thành viên theo điều lệ");
            }
            if (maxMembers != null && memberCount > maxMembers) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Đội '" + registration.getParticipantName() + "' có " + memberCount
                                + " thành viên, vượt quá tối đa " + maxMembers + " thành viên theo điều lệ");
            }
            java.util.Set<Long> candidateAthleteIds = new java.util.HashSet<>();
            if (members != null) {
                for (Map<String, Object> m : members) {
                    Long aid = parseLongField(m.get("athleteId"));
                    if (aid == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Thành viên đội '" + registration.getParticipantName() + "' thiếu athleteId hợp lệ");
                    }
                    Map<String, Object> memberAthlete = tournamentClient.getAthleteById(aid);
                    if (memberAthlete == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Không tìm thấy hồ sơ VĐV #" + aid + " trong đội");
                    }
                    String health = memberAthlete.get("healthStatus") != null
                            ? String.valueOf(memberAthlete.get("healthStatus")).trim() : "";
                    if (!"QUALIFIED".equalsIgnoreCase(health)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Thành viên VĐV #" + aid + " (" + memberAthlete.get("fullName")
                                        + ") chưa đạt trạng thái sức khỏe QUALIFIED (hiện tại: " + health + ")");
                    }
                    candidateAthleteIds.add(aid);
                }
            }
            if (!candidateAthleteIds.isEmpty()) {
                List<Registration> otherActiveTeamRegs = registrationRepository
                        .findByCompetitionIdAndParticipantTypeAndStatusIn(
                                registration.getCompetitionId(), "TEAM", List.of("PENDING", "APPROVED"));
                for (Registration other : otherActiveTeamRegs) {
                    if (java.util.Objects.equals(other.getParticipantReferenceId(), registration.getParticipantReferenceId())) {
                        continue;
                    }
                    List<Map<String, Object>> otherMembers = tournamentClient.getTeamMembers(other.getParticipantReferenceId());
                    if (otherMembers != null) {
                        for (Map<String, Object> om : otherMembers) {
                            Long otherAid = parseLongField(om.get("athleteId"));
                            if (otherAid != null && candidateAthleteIds.contains(otherAid)) {
                                throw new ResponseStatusException(HttpStatus.CONFLICT,
                                        "Vận động viên #" + otherAid + " đã nằm trong đội '" + other.getParticipantName()
                                                + "' (trạng thái " + other.getStatus() + ") ở cùng nội dung thi đấu #"
                                                + registration.getCompetitionId());
                            }
                        }
                    }
                }
            }
        } else {
            Map<String, Object> athlete = tournamentClient.getAthleteById(registration.getParticipantReferenceId());
            if (athlete == null) {
                throw new ResourceNotFoundException("Athlete not found with id: " + registration.getParticipantReferenceId());
            }
            if (!isAdmin) {
                String owner = athlete.get("ownerUsername") != null ? String.valueOf(athlete.get("ownerUsername")) : null;
                if (owner == null || owner.isBlank() || !owner.equalsIgnoreCase(callerUsername)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Access Denied: Bạn chỉ được phép đăng ký vận động viên thuộc quyền sở hữu của mình");
                }
            }
            String health = athlete.get("healthStatus") != null ? String.valueOf(athlete.get("healthStatus")).trim() : "";
            if (!"QUALIFIED".equalsIgnoreCase(health)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Vận động viên chưa đạt trạng thái sức khỏe QUALIFIED (hiện tại: " + health + ")");
            }
            if (athlete.get("fullName") != null && !String.valueOf(athlete.get("fullName")).isBlank()) {
                registration.setParticipantName(String.valueOf(athlete.get("fullName")));
            }
        }

        // 5. Duplicate check -> 409 Conflict for PENDING/APPROVED; allow re-register for REJECTED/WITHDRAWN
        Optional<Registration> existingOpt = registrationRepository
                .findByCompetitionIdAndParticipantTypeAndParticipantReferenceId(
                        registration.getCompetitionId(), type, registration.getParticipantReferenceId());
        if (existingOpt.isPresent()) {
            Registration existing = existingOpt.get();
            String st = existing.getStatus() != null ? existing.getStatus().trim().toUpperCase(Locale.ROOT) : "";
            if ("REJECTED".equals(st) || "WITHDRAWN".equals(st)) {
                existing.setParticipantName(registration.getParticipantName());
                existing.setRegisteredBy(callerUsername);
                existing.setStatus("PENDING");
                existing.setRejectionReason(null);
                existing.setRegistrationDate(LocalDateTime.now());
                return registrationRepository.save(existing);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Đối tượng đã đăng ký nội dung này với trạng thái: " + existing.getStatus());
        }

        registration.setId(null);
        registration.setParticipantType(type);
        registration.setRegisteredBy(callerUsername);
        registration.setStatus("PENDING");
        registration.setRejectionReason(null);
        registration.setRegistrationDate(LocalDateTime.now());

        return registrationRepository.save(registration);
    }

    @Transactional
    public Registration approveRegistration(Long id) {
        return approveRegistration(id, "admin", true);
    }

    @Transactional
    public Registration approveRegistration(Long id, String actorUsername, boolean isAdmin) {
        Registration reg = getRegistrationById(id);
        assertOrganizerOrAdminOwnership(reg.getCompetitionId(), actorUsername, isAdmin);

        if (!"PENDING".equalsIgnoreCase(reg.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chỉ có thể duyệt đơn đăng ký đang ở trạng thái PENDING (hiện tại: " + reg.getStatus() + ")");
        }
        if (matchRepository != null && !matchRepository.findByCompetitionId(reg.getCompetitionId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Nội dung thi đấu #" + reg.getCompetitionId() + " đã có lịch thi đấu, không thể duyệt thêm đơn đăng ký");
        }

        Map<String, Object> rule = tournamentClient.getCompetitionRule(reg.getCompetitionId());
        Integer maxParticipants = rule != null ? parseIntField(rule.get("maxParticipants")) : null;
        if (maxParticipants != null && maxParticipants > 0) {
            long currentCount = participantRepository.findByCompetitionId(reg.getCompetitionId()).size();
            if (currentCount >= maxParticipants) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Nội dung thi đấu #" + reg.getCompetitionId() + " đã đạt giới hạn tối đa " + maxParticipants + " đối tượng tham dự");
            }
        }

        reg.setStatus("APPROVED");
        reg.setRejectionReason(null);
        Registration savedReg = registrationRepository.save(reg);

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
        return rejectRegistration(id, reason, "admin", true);
    }

    @Transactional
    public Registration rejectRegistration(Long id, String reason, String actorUsername, boolean isAdmin) {
        Registration reg = getRegistrationById(id);
        assertOrganizerOrAdminOwnership(reg.getCompetitionId(), actorUsername, isAdmin);

        if ("WITHDRAWN".equalsIgnoreCase(reg.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể từ chối đơn đăng ký đã được rút");
        }

        if ("APPROVED".equalsIgnoreCase(reg.getStatus())) {
            deleteParticipantIfNoMatches(reg);
        }

        reg.setStatus("REJECTED");
        reg.setRejectionReason(reason);
        return registrationRepository.save(reg);
    }

    @Transactional
    public Registration withdrawRegistration(Long id, String actorUsername, boolean isAdmin) {
        if (actorUsername == null || actorUsername.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Full authentication is required to withdraw registration");
        }
        Registration reg = getRegistrationById(id);

        if (!isAdmin) {
            boolean isSubmitter = reg.getRegisteredBy() != null && reg.getRegisteredBy().equalsIgnoreCase(actorUsername);
            if (!isSubmitter) {
                boolean isLegacyOwner = false;
                if (reg.getRegisteredBy() == null || reg.getRegisteredBy().isBlank()) {
                    isLegacyOwner = checkEntityOwnershipFallback(reg, actorUsername);
                }
                if (!isLegacyOwner) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Access Denied: Bạn chỉ có thể rút đơn đăng ký do tài khoản của mình nộp");
                }
            }
        }

        String st = reg.getStatus() != null ? reg.getStatus().trim().toUpperCase(Locale.ROOT) : "";
        if ("REJECTED".equals(st) || "WITHDRAWN".equals(st)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Đơn đăng ký ở trạng thái " + reg.getStatus() + " không thể rút");
        }
        if ("APPROVED".equals(st)) {
            deleteParticipantIfNoMatches(reg);
        }

        reg.setStatus("WITHDRAWN");
        reg.setRejectionReason(null);
        return registrationRepository.save(reg);
    }

    private void assertOrganizerOrAdminOwnership(Long competitionId, String actorUsername, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (actorUsername == null || actorUsername.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Full authentication is required");
        }
        Map<String, Object> comp = tournamentClient.getCompetitionById(competitionId);
        if (comp == null || comp.get("tournamentId") == null) {
            throw new ResourceNotFoundException("Competition not found with id: " + competitionId);
        }
        Long tournamentId = parseLongField(comp.get("tournamentId"));
        Map<String, Object> tour = tournamentClient.getTournamentById(tournamentId);
        String owner = tour != null && tour.get("organizerUsername") != null
                ? String.valueOf(tour.get("organizerUsername"))
                : null;
        if (owner == null || owner.isBlank() || !owner.equalsIgnoreCase(actorUsername)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access Denied: Bạn không phải ban tổ chức sở hữu giải đấu #" + tournamentId);
        }
    }

    private void deleteParticipantIfNoMatches(Registration reg) {
        Optional<Participant> participantOpt = participantRepository.findByCompetitionIdAndParticipantTypeAndReferenceId(
                reg.getCompetitionId(), reg.getParticipantType(), reg.getParticipantReferenceId());
        if (participantOpt.isEmpty()) {
            return;
        }
        Participant participant = participantOpt.get();
        if (matchRepository != null) {
            boolean hasMatchById = participant.getId() != null
                    && matchRepository.existsByParticipant1IdOrParticipant2Id(participant.getId(), participant.getId());
            boolean hasMatchByName = participant.getDisplayName() != null
                    && matchRepository.findByCompetitionId(reg.getCompetitionId()).stream().anyMatch(m ->
                            (m.getParticipant1() != null && m.getParticipant1().equalsIgnoreCase(participant.getDisplayName()))
                                    || (m.getParticipant2() != null && m.getParticipant2().equalsIgnoreCase(participant.getDisplayName()))
                    );
            if (hasMatchById || hasMatchByName) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Không thể hủy tư cách tham dự vì '" + participant.getDisplayName() + "' đã có lịch thi đấu");
            }
        }
        participantRepository.delete(participant);
    }

    private boolean checkEntityOwnershipFallback(Registration reg, String actorUsername) {
        try {
            Map<String, Object> entity = "TEAM".equalsIgnoreCase(reg.getParticipantType())
                    ? tournamentClient.getTeamById(reg.getParticipantReferenceId())
                    : tournamentClient.getAthleteById(reg.getParticipantReferenceId());
            Object owner = entity != null ? entity.get("ownerUsername") : null;
            return owner != null && String.valueOf(owner).equalsIgnoreCase(actorUsername);
        } catch (Exception ex) {
            return false;
        }
    }

    private Long parseLongField(Object raw) {
        if (raw == null) return null;
        try {
            return Long.valueOf(String.valueOf(raw));
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer parseIntField(Object raw) {
        if (raw == null) return null;
        try {
            return Integer.valueOf(String.valueOf(raw));
        } catch (Exception ex) {
            return null;
        }
    }

    private LocalDate parseLocalDate(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDate ld) return ld;
        if (val instanceof LocalDateTime ldt) return ldt.toLocalDate();
        if (val instanceof List<?> arr && arr.size() >= 3) {
            return LocalDate.of(
                    Integer.parseInt(String.valueOf(arr.get(0))),
                    Integer.parseInt(String.valueOf(arr.get(1))),
                    Integer.parseInt(String.valueOf(arr.get(2)))
            );
        }
        String text = String.valueOf(val).trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) return null;
        if (text.length() >= 10) text = text.substring(0, 10);
        try {
            return LocalDate.parse(text);
        } catch (Exception ex) {
            return null;
        }
    }

    private LocalDateTime parseLocalDateTime(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDateTime ldt) return ldt;
        if (val instanceof LocalDate ld) return ld.atTime(23, 59, 59);
        if (val instanceof List<?> arr && arr.size() >= 3) {
            int y = Integer.parseInt(String.valueOf(arr.get(0)));
            int m = Integer.parseInt(String.valueOf(arr.get(1)));
            int d = Integer.parseInt(String.valueOf(arr.get(2)));
            int h = arr.size() >= 4 ? Integer.parseInt(String.valueOf(arr.get(3))) : 23;
            int min = arr.size() >= 5 ? Integer.parseInt(String.valueOf(arr.get(4))) : 59;
            int sec = arr.size() >= 6 ? Integer.parseInt(String.valueOf(arr.get(5))) : 59;
            return LocalDateTime.of(y, m, d, h, min, sec);
        }
        String text = String.valueOf(val).trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) return null;
        try {
            if (text.length() == 10) {
                return LocalDate.parse(text).atTime(23, 59, 59);
            }
            return LocalDateTime.parse(text);
        } catch (Exception ex) {
            return null;
        }
    }

    public Map<String, Object> getParticipantUsage(String participantType, Long referenceId) {
        if (participantType == null || participantType.isBlank() || referenceId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "participantType và referenceId là bắt buộc");
        }
        String normalizedType = participantType.trim().toUpperCase(Locale.ROOT);
        boolean hasActiveRegistrations = registrationRepository.existsByParticipantTypeAndParticipantReferenceIdAndStatusIn(
                normalizedType, referenceId, List.of("PENDING", "APPROVED")
        );
        boolean hasParticipants = participantRepository.existsByParticipantTypeAndReferenceId(normalizedType, referenceId);
        return Map.of(
                "participantType", normalizedType,
                "referenceId", referenceId,
                "hasActiveRegistrations", hasActiveRegistrations,
                "hasParticipants", hasParticipants,
                "inUse", hasActiveRegistrations || hasParticipants
        );
    }
}

