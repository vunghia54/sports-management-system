package com.sports.management.match.service;

import com.sports.management.exception.ResourceNotFoundException;
import com.sports.management.exception.ScheduleConflictException;
import com.sports.management.match.client.TournamentClient;
import com.sports.management.match.model.Match;
import com.sports.management.match.model.Participant;
import com.sports.management.match.repository.MatchRepository;
import com.sports.management.match.repository.ParticipantRepository;
import com.sports.management.messaging.event.MatchFinishedEvent;
import com.sports.management.messaging.producer.MatchEventProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class MatchService {

    public static final Set<String> ALLOWED_STATUSES = Set.of(
            "SCHEDULED", "ONGOING", "FINISHED", "CANCELLED", "POSTPONED"
    );

    public record ScheduleGenerationResult(
            List<Match> matches,
            List<Map<String, Object>> byeParticipants,
            String message,
            String formatType
    ) {
        public ScheduleGenerationResult(
                List<Match> matches,
                List<Map<String, Object>> byeParticipants,
                String message) {
            this(matches, byeParticipants, message, null);
        }
    }

    private final MatchRepository matchRepository;
    private final ParticipantRepository participantRepository;
    private final TournamentClient tournamentClient;
    private final MatchEventProducer matchEventProducer;
    private final ApplicationEventPublisher eventPublisher;

    public MatchService(
            MatchRepository matchRepository,
            TournamentClient tournamentClient,
            MatchEventProducer matchEventProducer) {
        this(matchRepository, null, tournamentClient, matchEventProducer, null);
    }

    @Autowired
    public MatchService(
            MatchRepository matchRepository,
            ParticipantRepository participantRepository,
            TournamentClient tournamentClient,
            MatchEventProducer matchEventProducer,
            ApplicationEventPublisher eventPublisher) {

        this.matchRepository = matchRepository;
        this.participantRepository = participantRepository;
        this.tournamentClient = tournamentClient;
        this.matchEventProducer = matchEventProducer;
        this.eventPublisher = eventPublisher;
    }

    // GET ALL
    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }

    public Page<Match> searchMatches(
            Long tournamentId,
            Long competitionId,
            String status,
            String keyword,
            Pageable pageable) {
        return searchMatches(tournamentId, competitionId, status, keyword, null, pageable);
    }

    public Page<Match> searchMatches(
            Long tournamentId,
            Long competitionId,
            String status,
            String keyword,
            LocalDate matchDate,
            Pageable pageable) {
        String safeStatus = (status != null && !status.isBlank()) ? status.trim() : null;
        String safeKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        LocalDateTime startOfDay = matchDate != null ? matchDate.atStartOfDay() : null;
        LocalDateTime endOfDay = matchDate != null ? matchDate.plusDays(1).atStartOfDay() : null;
        return matchRepository.searchMatches(tournamentId, competitionId, safeStatus, safeKeyword, startOfDay, endOfDay, pageable);
    }

    public Map<String, Object> getMatchStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalMatches", matchRepository.count());
        stats.put("scheduledMatches", matchRepository.countByStatusIgnoreCase("SCHEDULED"));
        stats.put("ongoingMatches", matchRepository.countByStatusIgnoreCase("ONGOING"));
        stats.put("finishedMatches", matchRepository.countByStatusIgnoreCase("FINISHED"));
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        long todayCount = matchRepository.findAll().stream()
                .filter(m -> m.getMatchTime() != null && !m.getMatchTime().isBefore(start) && m.getMatchTime().isBefore(end))
                .count();
        stats.put("todayMatches", todayCount);
        return stats;
    }

    // GET BY ID
    public Match getMatchById(Long id) {
        if (id == null) {
            return null;
        }
        return matchRepository.findById(id).orElse(null);
    }

    // KIEM TRA TOURNAMENT QUA HTTP REST (EUREKA SERVICE DISCOVERY)
    public boolean tournamentExists(Long tournamentId) {
        return tournamentClient.tournamentExists(tournamentId);
    }

    // GET MATCHES BY TOURNAMENT
    public List<Match> getMatchesByTournamentId(Long tournamentId) {
        return matchRepository.findByTournamentId(tournamentId);
    }

    // GET MATCHES BY COMPETITION
    public List<Match> getMatchesByCompetitionId(Long competitionId) {
        return matchRepository.findByCompetitionId(competitionId);
    }

    // CREATE MATCH
    @Transactional
    public Match createMatch(Match match) {
        if (match.getTournamentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tournament ID is required");
        }
        if (match.getCompetitionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competition ID is required");
        }
        if (match.getMatchTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Match time is required");
        }
        if (match.getParticipant1Id() == null || match.getParticipant2Id() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "participant1Id và participant2Id là bắt buộc từ danh sách tham dự");
        }

        match.setStatus(normalizeAndValidateStatus(match.getStatus(), true));

        Map<String, Object> tournament = requireTournamentMap(match.getTournamentId());
        if (tournament == null) {
            return null;
        }
        validateCompetitionBelongsToTournament(match.getCompetitionId(), match.getTournamentId());
        validateMatchTimeWithinTournament(match.getMatchTime(), tournament);
        resolveAndAssignParticipants(match);

        validateDistinctParticipants(match);
        if (!"CANCELLED".equalsIgnoreCase(match.getStatus())) {
            assertNoTimeSlotConflicts(match, null, matchRepository.findAll());
        }

        match.setId(null);
        Match savedMatch = matchRepository.save(match);
        System.out.println("[MATCH CREATED] ID = " + savedMatch.getId());
        return savedMatch;
    }

    // UPDATE MATCH
    @Transactional
    public Match updateMatch(Long id, Match updatedMatch) {
        Match existingMatch = getMatchById(id);
        if (existingMatch == null) {
            return null;
        }

        if (updatedMatch.getTournamentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tournament ID is required");
        }
        if (updatedMatch.getCompetitionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competition ID is required");
        }
        if (updatedMatch.getMatchTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Match time is required");
        }
        if (updatedMatch.getParticipant1Id() == null || updatedMatch.getParticipant2Id() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "participant1Id và participant2Id là bắt buộc từ danh sách tham dự");
        }

        String nextStatus = normalizeAndValidateStatus(
                updatedMatch.getStatus() != null ? updatedMatch.getStatus() : existingMatch.getStatus(), false);
        assertValidStatusTransition(existingMatch.getStatus(), nextStatus);

        Map<String, Object> tournament = requireTournamentMap(updatedMatch.getTournamentId());
        if (tournament == null) {
            return null;
        }
        validateCompetitionBelongsToTournament(updatedMatch.getCompetitionId(), updatedMatch.getTournamentId());
        validateMatchTimeWithinTournament(updatedMatch.getMatchTime(), tournament);

        Match candidate = copyMatch(existingMatch);
        candidate.setTournamentId(updatedMatch.getTournamentId());
        candidate.setCompetitionId(updatedMatch.getCompetitionId());
        candidate.setRound(updatedMatch.getRound());
        candidate.setGroupName(updatedMatch.getGroupName());
        candidate.setMatchTime(updatedMatch.getMatchTime());
        candidate.setLocation(updatedMatch.getLocation());
        candidate.setReferee(updatedMatch.getReferee());
        candidate.setRefereeUsername(updatedMatch.getRefereeUsername());
        candidate.setParticipant1Id(updatedMatch.getParticipant1Id());
        candidate.setParticipant2Id(updatedMatch.getParticipant2Id());
        candidate.setStatus(nextStatus);
        resolveAndAssignParticipants(candidate);

        assertFinishedMatchImmutableFields(existingMatch, candidate);
        validateDistinctParticipants(candidate);
        if (hasScheduleRelevantChanges(existingMatch, candidate)) {
            assertNoTimeSlotConflicts(candidate, id, matchRepository.findAll());
        }

        String oldStatus = existingMatch.getStatus();
        applyMatchValues(existingMatch, candidate);

        Match savedMatch = matchRepository.save(existingMatch);
        publishIfFinished(oldStatus, savedMatch);
        return savedMatch;
    }

    // PATCH MATCH
    @Transactional
    public Match patchMatch(Long id, Map<String, Object> updates) {
        Match existingMatch = getMatchById(id);
        if (existingMatch == null) {
            return null;
        }

        Match candidate = copyMatch(existingMatch);
        boolean validateTourAndComp = false;
        boolean validateTimeBounds = false;
        boolean resolveParticipants = false;

        if (updates.containsKey("tournamentId") && updates.get("tournamentId") != null) {
            Long nextTid = parseRequiredLong(updates.get("tournamentId"), "Invalid tournamentId");
            candidate.setTournamentId(nextTid);
            validateTourAndComp = true;
            validateTimeBounds = true;
        }
        if (updates.containsKey("competitionId")) {
            if (updates.get("competitionId") == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competition ID is required");
            }
            Long nextCid = parseRequiredLong(updates.get("competitionId"), "Invalid competitionId");
            candidate.setCompetitionId(nextCid);
            validateTourAndComp = true;
            if (candidate.getParticipant1Id() != null || candidate.getParticipant2Id() != null) {
                resolveParticipants = true;
            }
        }
        if (updates.containsKey("round") && updates.get("round") != null) {
            candidate.setRound(String.valueOf(updates.get("round")));
        }
        if (updates.containsKey("groupName") && updates.get("groupName") != null) {
            candidate.setGroupName(String.valueOf(updates.get("groupName")));
        }
        if (updates.containsKey("location") && updates.get("location") != null) {
            candidate.setLocation(String.valueOf(updates.get("location")));
        }
        if (updates.containsKey("referee") && updates.get("referee") != null) {
            candidate.setReferee(String.valueOf(updates.get("referee")));
        }
        if (updates.containsKey("refereeUsername")) {
            Object val = updates.get("refereeUsername");
            candidate.setRefereeUsername(val != null && !String.valueOf(val).isBlank() ? String.valueOf(val).trim() : null);
        }
        if (updates.containsKey("matchTime")) {
            if (updates.get("matchTime") == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian thi đấu không được để trống");
            }
            try {
                candidate.setMatchTime(LocalDateTime.parse(String.valueOf(updates.get("matchTime"))));
                validateTimeBounds = true;
            } catch (Exception ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Định dạng matchTime không hợp lệ (ISO-8601)");
            }
        }
        if (updates.containsKey("participant1Id")) {
            if (updates.get("participant1Id") == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "participant1Id không được để trống");
            }
            candidate.setParticipant1Id(parseRequiredLong(updates.get("participant1Id"), "Invalid participant1Id"));
            resolveParticipants = true;
        }
        if (updates.containsKey("participant2Id")) {
            if (updates.get("participant2Id") == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "participant2Id không được để trống");
            }
            candidate.setParticipant2Id(parseRequiredLong(updates.get("participant2Id"), "Invalid participant2Id"));
            resolveParticipants = true;
        }
        if ((updates.containsKey("participant1") && !updates.containsKey("participant1Id"))
                || (updates.containsKey("participant2") && !updates.containsKey("participant2Id"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không thể chỉnh sửa tên đội trực tiếp; hãy truyền participant1Id/participant2Id từ danh sách tham dự");
        }
        if (updates.containsKey("status") && updates.get("status") != null) {
            String nextStatus = normalizeAndValidateStatus(String.valueOf(updates.get("status")), false);
            assertValidStatusTransition(existingMatch.getStatus(), nextStatus);
            candidate.setStatus(nextStatus);
        }

        if (validateTourAndComp || validateTimeBounds) {
            Map<String, Object> tournament = requireTournamentMap(candidate.getTournamentId());
            if (tournament == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tournament ID");
            }
            if (validateTourAndComp) {
                if (candidate.getCompetitionId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competition ID is required");
                }
                validateCompetitionBelongsToTournament(candidate.getCompetitionId(), candidate.getTournamentId());
            }
            if (validateTimeBounds) {
                validateMatchTimeWithinTournament(candidate.getMatchTime(), tournament);
            }
        }
        if (resolveParticipants) {
            if (candidate.getCompetitionId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competition ID is required");
            }
            if (candidate.getParticipant1Id() == null || candidate.getParticipant2Id() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "participant1Id và participant2Id là bắt buộc");
            }
            resolveAndAssignParticipants(candidate);
        }

        assertFinishedMatchImmutableFields(existingMatch, candidate);
        validateDistinctParticipants(candidate);
        if (hasScheduleRelevantChanges(existingMatch, candidate)) {
            assertNoTimeSlotConflicts(candidate, id, matchRepository.findAll());
        }

        String oldStatus = existingMatch.getStatus();
        applyMatchValues(existingMatch, candidate);

        Match savedMatch = matchRepository.save(existingMatch);
        publishIfFinished(oldStatus, savedMatch);
        return savedMatch;
    }

    public List<Map<String, Object>> checkScheduleConflicts(Match candidate, Long excludeMatchId) {
        return detectScheduleConflicts(candidate, excludeMatchId, matchRepository.findAll());
    }

    private List<Map<String, Object>> detectScheduleConflicts(Match candidate, Long excludeMatchId, Collection<Match> pool) {
        List<Map<String, Object>> conflicts = new ArrayList<>();
        if (candidate.getParticipant1() != null && candidate.getParticipant2() != null
                && !candidate.getParticipant1().isBlank()
                && candidate.getParticipant1().trim().equalsIgnoreCase(candidate.getParticipant2().trim())) {
            conflicts.add(Map.of(
                    "type", "SAME_PARTICIPANT",
                    "message", "Đội / VĐV 1 và Đội / VĐV 2 không được trùng nhau"
            ));
        }
        if ("CANCELLED".equalsIgnoreCase(candidate.getStatus()) || candidate.getMatchTime() == null) {
            return conflicts;
        }

        for (Match m : pool) {
            if (excludeMatchId != null && excludeMatchId.equals(m.getId())) continue;
            if (m == candidate) continue;
            if ("CANCELLED".equalsIgnoreCase(m.getStatus()) || m.getMatchTime() == null) continue;

            long minutesDiff = Math.abs(java.time.Duration.between(candidate.getMatchTime(), m.getMatchTime()).toMinutes());
            if (minutesDiff < 120) {
                if (candidate.getLocation() != null && !candidate.getLocation().isBlank()
                        && m.getLocation() != null
                        && candidate.getLocation().trim().equalsIgnoreCase(m.getLocation().trim())) {
                    Map<String, Object> c = new LinkedHashMap<>();
                    c.put("type", "VENUE_CONFLICT");
                    if (m.getId() != null) c.put("matchId", m.getId());
                    c.put("message", "Trùng sân thi đấu '" + m.getLocation() + "' với trận " + m.getParticipant1() + " vs " + m.getParticipant2() + " lúc " + m.getMatchTime());
                    conflicts.add(c);
                }

                boolean refUsernameConflict = candidate.getRefereeUsername() != null && !candidate.getRefereeUsername().isBlank()
                        && m.getRefereeUsername() != null && !m.getRefereeUsername().isBlank()
                        && candidate.getRefereeUsername().trim().equalsIgnoreCase(m.getRefereeUsername().trim());
                boolean refDisplayConflict = candidate.getReferee() != null && !candidate.getReferee().isBlank()
                        && m.getReferee() != null && !m.getReferee().isBlank()
                        && candidate.getReferee().trim().equalsIgnoreCase(m.getReferee().trim());
                if (refUsernameConflict || refDisplayConflict) {
                    String refLabel = (candidate.getRefereeUsername() != null && !candidate.getRefereeUsername().isBlank())
                            ? candidate.getRefereeUsername() : candidate.getReferee();
                    Map<String, Object> c = new LinkedHashMap<>();
                    c.put("type", "REFEREE_CONFLICT");
                    if (m.getId() != null) c.put("matchId", m.getId());
                    c.put("message", "Trọng tài '" + refLabel + "' đang bắt trận " + m.getParticipant1() + " vs " + m.getParticipant2() + " lúc " + m.getMatchTime());
                    conflicts.add(c);
                }

                String cp1 = candidate.getParticipant1() != null ? candidate.getParticipant1().trim() : "";
                String cp2 = candidate.getParticipant2() != null ? candidate.getParticipant2().trim() : "";
                String mp1 = m.getParticipant1() != null ? m.getParticipant1().trim() : "";
                String mp2 = m.getParticipant2() != null ? m.getParticipant2().trim() : "";
                boolean sameTeamById = (candidate.getParticipant1Id() != null && (candidate.getParticipant1Id().equals(m.getParticipant1Id()) || candidate.getParticipant1Id().equals(m.getParticipant2Id())))
                        || (candidate.getParticipant2Id() != null && (candidate.getParticipant2Id().equals(m.getParticipant1Id()) || candidate.getParticipant2Id().equals(m.getParticipant2Id())));
                boolean sameTeamByName = (!cp1.isBlank() && (cp1.equalsIgnoreCase(mp1) || cp1.equalsIgnoreCase(mp2)))
                        || (!cp2.isBlank() && (cp2.equalsIgnoreCase(mp1) || cp2.equalsIgnoreCase(mp2)));
                if (sameTeamById || sameTeamByName) {
                    Map<String, Object> c = new LinkedHashMap<>();
                    c.put("type", "TEAM_CONFLICT");
                    if (m.getId() != null) c.put("matchId", m.getId());
                    c.put("message", "Đội tuyển bị trùng khung giờ thi đấu với trận " + m.getParticipant1() + " vs " + m.getParticipant2() + " lúc " + m.getMatchTime());
                    conflicts.add(c);
                }
            }
        }
        return conflicts;
    }

    private void validateDistinctParticipants(Match candidate) {
        if (candidate.getParticipant1Id() != null && candidate.getParticipant2Id() != null
                && Objects.equals(candidate.getParticipant1Id(), candidate.getParticipant2Id())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hai participant tham gia trận đấu không được trùng nhau");
        }
        if (candidate.getParticipant1() != null && candidate.getParticipant2() != null
                && !candidate.getParticipant1().isBlank()
                && candidate.getParticipant1().trim().equalsIgnoreCase(candidate.getParticipant2().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hai đội tham gia trận đấu không được trùng nhau");
        }
    }

    private void assertNoTimeSlotConflicts(Match candidate, Long excludeMatchId, Collection<Match> pool) {
        List<Map<String, Object>> slotConflicts = detectScheduleConflicts(candidate, excludeMatchId, pool).stream()
                .filter(c -> !"SAME_PARTICIPANT".equals(c.get("type")))
                .toList();
        if (!slotConflicts.isEmpty()) {
            String summary = slotConflicts.stream()
                    .map(c -> String.valueOf(c.get("message")))
                    .distinct()
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Lịch thi đấu bị xung đột");
            throw new ScheduleConflictException(summary, slotConflicts);
        }
    }

    private boolean hasScheduleRelevantChanges(Match existing, Match candidate) {
        if ("CANCELLED".equalsIgnoreCase(candidate.getStatus())) {
            return false;
        }
        if ("CANCELLED".equalsIgnoreCase(existing.getStatus())) {
            return true;
        }
        return !Objects.equals(existing.getMatchTime(), candidate.getMatchTime())
                || !Objects.equals(norm(existing.getLocation()), norm(candidate.getLocation()))
                || !Objects.equals(norm(existing.getReferee()), norm(candidate.getReferee()))
                || !Objects.equals(norm(existing.getRefereeUsername()), norm(candidate.getRefereeUsername()))
                || !Objects.equals(norm(existing.getParticipant1()), norm(candidate.getParticipant1()))
                || !Objects.equals(norm(existing.getParticipant2()), norm(candidate.getParticipant2()));
    }

    private String norm(String val) {
        return val == null || val.isBlank() ? null : val.trim().toLowerCase();
    }

    private Match copyMatch(Match src) {
        Match copy = new Match();
        copy.setId(src.getId());
        copy.setTournamentId(src.getTournamentId());
        copy.setCompetitionId(src.getCompetitionId());
        copy.setRound(src.getRound());
        copy.setGroupName(src.getGroupName());
        copy.setMatchTime(src.getMatchTime());
        copy.setLocation(src.getLocation());
        copy.setReferee(src.getReferee());
        copy.setRefereeUsername(src.getRefereeUsername());
        copy.setParticipant1Id(src.getParticipant1Id());
        copy.setParticipant2Id(src.getParticipant2Id());
        copy.setParticipant1(src.getParticipant1());
        copy.setParticipant2(src.getParticipant2());
        copy.setStatus(src.getStatus());
        copy.setOwnerUsername(src.getOwnerUsername());
        return copy;
    }

    private void applyMatchValues(Match target, Match source) {
        target.setTournamentId(source.getTournamentId());
        target.setCompetitionId(source.getCompetitionId());
        target.setRound(source.getRound());
        target.setGroupName(source.getGroupName());
        target.setMatchTime(source.getMatchTime());
        target.setLocation(source.getLocation());
        target.setReferee(source.getReferee());
        target.setRefereeUsername(source.getRefereeUsername());
        target.setParticipant1Id(source.getParticipant1Id());
        target.setParticipant2Id(source.getParticipant2Id());
        target.setParticipant1(source.getParticipant1());
        target.setParticipant2(source.getParticipant2());
        target.setStatus(source.getStatus());
    }

    private Long parseRequiredLong(Object raw, String errMessage) {
        try {
            return Long.valueOf(String.valueOf(raw));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMessage);
        }
    }

    private Map<String, Object> requireTournamentMap(Long tournamentId) {
        if (tournamentId == null) {
            return null;
        }
        Map<String, Object> tournament = tournamentClient.getTournamentById(tournamentId);
        if (tournament == null) {
            return null;
        }
        return tournament;
    }

    private void validateCompetitionBelongsToTournament(Long competitionId, Long tournamentId) {
        if (competitionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competition ID is required");
        }
        Map<String, Object> competition;
        try {
            competition = tournamentClient.getCompetitionById(competitionId);
        } catch (ResourceNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung thi đấu không tồn tại với id: " + competitionId);
        }
        if (competition == null || competition.get("tournamentId") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung thi đấu #" + competitionId + " không hợp lệ");
        }
        Long compTournamentId = parseRequiredLong(competition.get("tournamentId"), "Nội dung thi đấu không có tournamentId hợp lệ");
        if (!Objects.equals(compTournamentId, tournamentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nội dung thi đấu #" + competitionId + " không thuộc giải đấu #" + tournamentId);
        }
    }

    private void validateMatchTimeWithinTournament(LocalDateTime matchTime, Map<String, Object> tournament) {
        if (matchTime == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian thi đấu không được để trống");
        }
        if (tournament == null) {
            return;
        }
        LocalDate startDate = parseLocalDate(tournament.get("startDate"));
        LocalDate endDate = parseLocalDate(tournament.get("endDate"));
        LocalDate matchDate = matchTime.toLocalDate();
        if (startDate != null && matchDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Thời gian thi đấu (" + matchDate + ") không được trước ngày khai mạc giải (" + startDate + ")");
        }
        if (endDate != null && matchDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Thời gian thi đấu (" + matchDate + ") không được sau ngày bế mạc giải (" + endDate + ")");
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

    private void resolveAndAssignParticipants(Match match) {
        if (participantRepository == null) {
            throw new IllegalStateException("ParticipantRepository is not configured");
        }
        Participant p1 = participantRepository.findById(match.getParticipant1Id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Participant 1 không tồn tại với id: " + match.getParticipant1Id()));
        Participant p2 = participantRepository.findById(match.getParticipant2Id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Participant 2 không tồn tại với id: " + match.getParticipant2Id()));

        if (!Objects.equals(p1.getCompetitionId(), match.getCompetitionId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Participant #" + p1.getId() + " (" + p1.getDisplayName() + ") không thuộc nội dung thi đấu #" + match.getCompetitionId());
        }
        if (!Objects.equals(p2.getCompetitionId(), match.getCompetitionId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Participant #" + p2.getId() + " (" + p2.getDisplayName() + ") không thuộc nội dung thi đấu #" + match.getCompetitionId());
        }
        if (Objects.equals(p1.getId(), p2.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hai participant tham gia trận đấu không được trùng nhau");
        }
        match.setParticipant1(p1.getDisplayName());
        match.setParticipant2(p2.getDisplayName());
    }

    private String normalizeAndValidateStatus(String status, boolean defaultToScheduled) {
        if (status == null || status.isBlank()) {
            if (defaultToScheduled) {
                return "SCHEDULED";
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái trận đấu không được để trống");
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Trạng thái trận đấu không hợp lệ: " + status + ". Chỉ chấp nhận: " + ALLOWED_STATUSES);
        }
        return normalized;
    }

    private void assertValidStatusTransition(String oldRaw, String nextRaw) {
        String current = oldRaw != null && !oldRaw.isBlank() ? oldRaw.trim().toUpperCase(Locale.ROOT) : "SCHEDULED";
        String next = nextRaw != null ? nextRaw.trim().toUpperCase(Locale.ROOT) : current;
        if (current.equals(next)) {
            return;
        }
        Set<String> allowedTargets = switch (current) {
            case "SCHEDULED" -> Set.of("SCHEDULED", "ONGOING", "FINISHED", "CANCELLED", "POSTPONED");
            case "POSTPONED" -> Set.of("POSTPONED", "SCHEDULED", "ONGOING", "CANCELLED");
            case "ONGOING" -> Set.of("ONGOING", "FINISHED", "CANCELLED", "POSTPONED");
            case "FINISHED" -> Set.of("FINISHED");
            case "CANCELLED" -> Set.of("CANCELLED");
            default -> ALLOWED_STATUSES;
        };
        if (!allowedTargets.contains(next)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không thể chuyển trạng thái trận đấu từ " + current + " sang " + next);
        }
    }

    private void assertFinishedMatchImmutableFields(Match existing, Match candidate) {
        if (!"FINISHED".equalsIgnoreCase(existing.getStatus())) {
            return;
        }
        boolean modifiedProtected = !Objects.equals(existing.getTournamentId(), candidate.getTournamentId())
                || !Objects.equals(existing.getCompetitionId(), candidate.getCompetitionId())
                || !Objects.equals(existing.getParticipant1Id(), candidate.getParticipant1Id())
                || !Objects.equals(existing.getParticipant2Id(), candidate.getParticipant2Id())
                || !Objects.equals(norm(existing.getParticipant1()), norm(candidate.getParticipant1()))
                || !Objects.equals(norm(existing.getParticipant2()), norm(candidate.getParticipant2()))
                || !Objects.equals(existing.getMatchTime(), candidate.getMatchTime())
                || !Objects.equals(norm(existing.getLocation()), norm(candidate.getLocation()));
        if (modifiedProtected) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Trận đấu đã kết thúc (FINISHED), không được sửa đội tham gia, thời gian hoặc sân thi đấu");
        }
    }

    @Transactional
    public ScheduleGenerationResult generateScheduleFromParticipants(
            Long tournamentId,
            Long competitionId,
            String clientRequestedFormat,
            String startDateStr,
            List<String> venues,
            List<String> referees,
            String creatorUsername) {

        if (tournamentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tournament ID is required");
        }
        if (competitionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competition ID is required");
        }

        Map<String, Object> tournament = requireTournamentMap(tournamentId);
        if (tournament == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tournament ID: " + tournamentId);
        }
        validateCompetitionBelongsToTournament(competitionId, tournamentId);

        Map<String, Object> formatConfig = tournamentClient.getCompetitionFormat(competitionId);
        if (formatConfig == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nội dung thi đấu #" + competitionId + " chưa có cấu hình thể thức thi đấu");
        }
        String fmt = normalizeFormatType(
                formatConfig.get("formatType") != null ? String.valueOf(formatConfig.get("formatType")) : "KNOCKOUT");
        if (clientRequestedFormat != null && !clientRequestedFormat.isBlank()) {
            String requestedFmt = normalizeFormatType(clientRequestedFormat);
            if (!requestedFmt.equals(fmt)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không truyền thể thức thi đấu ('" + requestedFmt + "') khác với cấu hình nội dung thi đấu ('" + fmt
                                + "'). Vui lòng sửa thể thức tại cấu hình nội dung thi đấu.");
            }
        }
        if ("HYBRID".equals(fmt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Thể thức HYBRID chưa hỗ trợ xếp lịch tự động");
        }
        int numberOfGroups = parsePositiveInt(formatConfig.get("numberOfGroups"), 1);

        if (!matchRepository.findByCompetitionId(competitionId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Nội dung thi đấu #" + competitionId + " đã có lịch thi đấu. Không thể tự động xếp lịch trùng.");
        }

        List<Participant> participants = participantRepository == null
                ? List.of()
                : participantRepository.findByCompetitionId(competitionId);
        if (participants.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cần ít nhất 2 đội/VĐV đã duyệt trong danh sách tham dự của nội dung thi đấu #" + competitionId + " để xếp lịch");
        }

        LocalDate tourStart = parseLocalDate(tournament.get("startDate"));
        LocalDate tourEnd = parseLocalDate(tournament.get("endDate"));
        LocalDate startDate;
        if (startDateStr != null && !startDateStr.isBlank()) {
            try {
                startDate = LocalDate.parse(startDateStr.trim());
            } catch (Exception ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Định dạng startDate không hợp lệ (YYYY-MM-DD)");
            }
        } else {
            LocalDate defaultStart = LocalDate.now().plusDays(1);
            startDate = (tourStart != null && defaultStart.isBefore(tourStart)) ? tourStart : defaultStart;
        }
        if (tourStart != null && startDate.isBefore(tourStart)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ngày bắt đầu xếp lịch (" + startDate + ") phải nằm trong thời gian giải đấu [" + tourStart + ", " + tourEnd + "]");
        }
        if (tourEnd != null && startDate.isAfter(tourEnd)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ngày bắt đầu xếp lịch (" + startDate + ") không được sau ngày kết thúc giải đấu (" + tourEnd + ")");
        }

        List<String> venuePool = (venues != null && !venues.isEmpty())
                ? venues
                : List.of("SVĐ Quốc Gia Mỹ Đình", "SVĐ Hàng Đẫy", "SVĐ Cẩm Phả");
        List<String> refPool = (referees != null && !referees.isEmpty())
                ? referees
                : List.of("TT FIFA Ngô Duy Lân", "TT Hoàng Ngọc Hà", "TT Nguyễn Mạnh Hải");

        List<Match> planned = new ArrayList<>();
        List<Map<String, Object>> byeParticipants = new ArrayList<>();

        if ("KNOCKOUT".equals(fmt)) {
            String roundLabel = resolveKnockoutRoundLabel(participants.size());
            int stride = Math.max(1, Math.min(venuePool.size(), refPool.size()));
            int roundNum = 1;
            int matchIdx = 0;
            for (int i = 0; i + 1 < participants.size(); i += 2) {
                Participant p1 = participants.get(i);
                Participant p2 = participants.get(i + 1);
                int dayOffset = matchIdx / (stride * 2);
                int slotInDay = (matchIdx % (stride * 2)) / stride;
                int lane = matchIdx % stride;

                Match m = new Match();
                m.setTournamentId(tournamentId);
                m.setCompetitionId(competitionId);
                m.setRound(roundLabel + " - Trận " + roundNum);
                m.setGroupName("Nhánh " + ((i / 2) + 1));
                m.setParticipant1Id(p1.getId());
                m.setParticipant2Id(p2.getId());
                m.setParticipant1(p1.getDisplayName());
                m.setParticipant2(p2.getDisplayName());
                m.setLocation(venuePool.get(lane));
                m.setReferee(refPool.get(lane));
                m.setMatchTime(startDate.plusDays(dayOffset).atTime(LocalTime.of(17, 0).plusMinutes(slotInDay * 150L)));
                m.setStatus("SCHEDULED");
                m.setOwnerUsername(creatorUsername);
                validateDistinctParticipants(m);
                planned.add(m);
                roundNum++;
                matchIdx++;
            }
            if (participants.size() % 2 == 1) {
                Participant byeTeam = participants.get(participants.size() - 1);
                Map<String, Object> byeInfo = new LinkedHashMap<>();
                byeInfo.put("id", byeTeam.getId());
                byeInfo.put("displayName", byeTeam.getDisplayName());
                byeInfo.put("round", roundLabel);
                byeInfo.put("reason", "Số lượng đội lẻ (" + participants.size() + "), được miễn thi đấu vòng đầu (" + roundLabel + ")");
                byeParticipants.add(byeInfo);
            }
        } else if ("GROUP_STAGE".equals(fmt)) {
            if (participants.size() < numberOfGroups * 2) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cần ít nhất " + (numberOfGroups * 2) + " đội/VĐV để chia thành " + numberOfGroups + " bảng đấu (mỗi bảng tối thiểu 2 đội)");
            }
            List<List<Participant>> groups = new ArrayList<>();
            for (int g = 0; g < numberOfGroups; g++) {
                groups.add(new ArrayList<>());
            }
            for (int i = 0; i < participants.size(); i++) {
                groups.get(i % numberOfGroups).add(participants.get(i));
            }
            int nextDayOffset = 0;
            for (int g = 0; g < numberOfGroups; g++) {
                String groupLabel = g < 26 ? ("Bảng " + (char) ('A' + g)) : ("Bảng " + (g + 1));
                nextDayOffset = appendCircleRoundRobinMatches(
                        planned, groups.get(g), tournamentId, competitionId, groupLabel,
                        startDate, nextDayOffset, venuePool, refPool, creatorUsername);
            }
        } else if ("ROUND_ROBIN".equals(fmt)) {
            appendCircleRoundRobinMatches(
                    planned, participants, tournamentId, competitionId, "Vòng tròn",
                    startDate, 0, venuePool, refPool, creatorUsername);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Thể thức thi đấu không được hỗ trợ xếp lịch tự động: " + fmt);
        }

        for (Match candidate : planned) {
            validateMatchTimeWithinTournament(candidate.getMatchTime(), tournament);
        }

        List<Match> existingMatches = matchRepository.findAll();
        List<Match> combinedPool = new ArrayList<>(existingMatches);
        List<Map<String, Object>> batchConflicts = new ArrayList<>();
        for (Match candidate : planned) {
            List<Map<String, Object>> found = detectScheduleConflicts(candidate, null, combinedPool).stream()
                    .filter(c -> !"SAME_PARTICIPANT".equals(c.get("type")))
                    .toList();
            if (!found.isEmpty()) {
                batchConflicts.addAll(found);
            }
            combinedPool.add(candidate);
        }
        if (!batchConflicts.isEmpty()) {
            String summary = batchConflicts.stream()
                    .map(c -> String.valueOf(c.get("message")))
                    .distinct()
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Sinh lịch bị xung đột với lịch hiện có");
            throw new ScheduleConflictException(summary, batchConflicts);
        }

        List<Match> saved = matchRepository.saveAll(planned);
        String msg = byeParticipants.isEmpty()
                ? "Đã tự động tạo " + saved.size() + " trận đấu theo thể thức " + fmt
                : "Đã tự động tạo " + saved.size() + " trận đấu theo thể thức " + fmt + " (Miễn vòng đầu: "
                + byeParticipants.stream().map(b -> String.valueOf(b.get("displayName"))).reduce((a, b) -> a + ", " + b).orElse("") + ")";
        return new ScheduleGenerationResult(saved, byeParticipants, msg, fmt);
    }

    private String normalizeFormatType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "KNOCKOUT";
        }
        String norm = raw.trim().toUpperCase(Locale.ROOT);
        if ("SINGLE_ELIMINATION".equals(norm)) {
            return "KNOCKOUT";
        }
        return norm;
    }

    private int parsePositiveInt(Object raw, int defaultVal) {
        if (raw == null) {
            return defaultVal;
        }
        try {
            int val = Integer.parseInt(String.valueOf(raw).trim());
            return val > 0 ? val : defaultVal;
        } catch (NumberFormatException ex) {
            return defaultVal;
        }
    }

    private int appendCircleRoundRobinMatches(
            List<Match> out,
            List<Participant> groupParticipants,
            Long tournamentId,
            Long competitionId,
            String groupLabel,
            LocalDate startDate,
            int initialDayOffset,
            List<String> venuePool,
            List<String> refPool,
            String creatorUsername) {

        if (groupParticipants.size() < 2) {
            return initialDayOffset;
        }
        List<Participant> rotation = new ArrayList<>(groupParticipants);
        if (rotation.size() % 2 != 0) {
            rotation.add(null); // BYE slot for odd number of teams
        }
        int m = rotation.size();
        int numRounds = m - 1;
        int half = m / 2;
        int stride = Math.max(1, Math.min(venuePool.size(), refPool.size()));
        int currentDayOffset = initialDayOffset;

        for (int roundIdx = 0; roundIdx < numRounds; roundIdx++) {
            int roundNum = roundIdx + 1;
            int matchInRound = 0;
            int maxSlotInRound = 0;

            for (int i = 0; i < half; i++) {
                Participant home = rotation.get(i);
                Participant away = rotation.get(m - 1 - i);
                if (home != null && away != null) {
                    if (roundIdx % 2 == 1 && i == 0) {
                        Participant tmp = home;
                        home = away;
                        away = tmp;
                    }
                    int subDay = matchInRound / (stride * 2);
                    int slotInDay = (matchInRound % (stride * 2)) / stride;
                    int lane = matchInRound % stride;
                    maxSlotInRound = Math.max(maxSlotInRound, subDay);

                    Match match = new Match();
                    match.setTournamentId(tournamentId);
                    match.setCompetitionId(competitionId);
                    match.setGroupName(groupLabel);
                    match.setRound(groupLabel + " - Lượt " + roundNum);
                    match.setParticipant1Id(home.getId());
                    match.setParticipant2Id(away.getId());
                    match.setParticipant1(home.getDisplayName());
                    match.setParticipant2(away.getDisplayName());
                    match.setLocation(venuePool.get(lane));
                    match.setReferee(refPool.get(lane));
                    match.setMatchTime(startDate.plusDays(currentDayOffset + subDay)
                            .atTime(LocalTime.of(17, 0).plusMinutes(slotInDay * 150L)));
                    match.setStatus("SCHEDULED");
                    match.setOwnerUsername(creatorUsername);
                    validateDistinctParticipants(match);
                    out.add(match);
                    matchInRound++;
                }
            }
            currentDayOffset += maxSlotInRound + 1;
            Participant last = rotation.remove(m - 1);
            rotation.add(1, last);
        }
        return currentDayOffset;
    }

    private String resolveKnockoutRoundLabel(int teamCount) {
        if (teamCount <= 2) return "Chung kết";
        if (teamCount <= 4) return "Bán kết";
        if (teamCount <= 8) return "Tứ kết";
        if (teamCount <= 16) return "Vòng 1/8";
        return "Vòng 1/16";
    }

    private void publishIfFinished(String oldStatus, Match savedMatch) {
        boolean wasFinished = "FINISHED".equalsIgnoreCase(oldStatus);
        boolean isFinished = "FINISHED".equalsIgnoreCase(savedMatch.getStatus());

        if (!wasFinished && isFinished) {
            MatchFinishedEvent event = new MatchFinishedEvent();
            event.setEvent("MATCH_FINISHED");
            event.setMatchId(savedMatch.getId());
            event.setTournamentId(savedMatch.getTournamentId());
            event.setParticipant1(savedMatch.getParticipant1());
            event.setParticipant2(savedMatch.getParticipant2());

            if (eventPublisher != null) {
                eventPublisher.publishEvent(event);
            } else {
                matchEventProducer.publishMatchFinished(event);
            }
            System.out.println("[MATCH FINISHED] Event queued for AFTER_COMMIT publish, Match ID = " + savedMatch.getId());
        }
    }

    // DELETE MATCH
    @Transactional
    public boolean deleteMatch(Long id) {
        if (id == null || !matchRepository.existsById(id)) {
            return false;
        }

        matchRepository.deleteById(id);
        return true;
    }
}
