package com.sports.management.match.service;

import com.sports.management.exception.ScheduleConflictException;
import com.sports.management.match.client.TournamentClient;
import com.sports.management.match.model.Match;
import com.sports.management.match.repository.MatchRepository;
import com.sports.management.messaging.event.MatchFinishedEvent;
import com.sports.management.messaging.producer.MatchEventProducer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final TournamentClient tournamentClient;
    private final MatchEventProducer matchEventProducer;

    public MatchService(
            MatchRepository matchRepository,
            TournamentClient tournamentClient,
            MatchEventProducer matchEventProducer) {

        this.matchRepository = matchRepository;
        this.tournamentClient = tournamentClient;
        this.matchEventProducer = matchEventProducer;
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
        validateDistinctParticipants(match);
        if (!tournamentExists(match.getTournamentId())) {
            return null;
        }
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

        if (!tournamentExists(updatedMatch.getTournamentId())) {
            return null;
        }

        Match candidate = copyMatch(existingMatch);
        candidate.setTournamentId(updatedMatch.getTournamentId());
        if (updatedMatch.getCompetitionId() != null) {
            candidate.setCompetitionId(updatedMatch.getCompetitionId());
        }
        candidate.setRound(updatedMatch.getRound());
        candidate.setGroupName(updatedMatch.getGroupName());
        candidate.setMatchTime(updatedMatch.getMatchTime());
        candidate.setLocation(updatedMatch.getLocation());
        candidate.setReferee(updatedMatch.getReferee());
        candidate.setRefereeUsername(updatedMatch.getRefereeUsername());
        candidate.setParticipant1(updatedMatch.getParticipant1());
        candidate.setParticipant2(updatedMatch.getParticipant2());
        candidate.setStatus(updatedMatch.getStatus());

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

        if (updates.containsKey("tournamentId") && updates.get("tournamentId") != null) {
            Long nextTid;
            try {
                nextTid = Long.valueOf(String.valueOf(updates.get("tournamentId")));
            } catch (NumberFormatException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tournamentId");
            }
            if (!tournamentExists(nextTid)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tournament ID");
            }
            candidate.setTournamentId(nextTid);
        }
        if (updates.containsKey("competitionId") && updates.get("competitionId") != null) {
            candidate.setCompetitionId(Long.valueOf(String.valueOf(updates.get("competitionId"))));
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
        if (updates.containsKey("matchTime") && updates.get("matchTime") != null) {
            try {
                candidate.setMatchTime(LocalDateTime.parse(String.valueOf(updates.get("matchTime"))));
            } catch (Exception ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Định dạng matchTime không hợp lệ (ISO-8601)");
            }
        }
        if (updates.containsKey("participant1") && updates.get("participant1") != null) {
            candidate.setParticipant1(String.valueOf(updates.get("participant1")));
        }
        if (updates.containsKey("participant2") && updates.get("participant2") != null) {
            candidate.setParticipant2(String.valueOf(updates.get("participant2")));
        }
        if (updates.containsKey("status") && updates.get("status") != null) {
            candidate.setStatus(String.valueOf(updates.get("status")));
        }

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
                boolean sameTeam = (!cp1.isBlank() && (cp1.equalsIgnoreCase(mp1) || cp1.equalsIgnoreCase(mp2)))
                        || (!cp2.isBlank() && (cp2.equalsIgnoreCase(mp1) || cp2.equalsIgnoreCase(mp2)));
                if (sameTeam) {
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
        target.setParticipant1(source.getParticipant1());
        target.setParticipant2(source.getParticipant2());
        target.setStatus(source.getStatus());
    }

    @Transactional
    public List<Match> generateSchedule(
            Long tournamentId,
            Long competitionId,
            String format,
            List<String> teams,
            String startDateStr,
            List<String> venues,
            List<String> referees) {
        return generateSchedule(tournamentId, competitionId, format, teams, startDateStr, venues, referees, null);
    }

    @Transactional
    public List<Match> generateSchedule(
            Long tournamentId,
            Long competitionId,
            String format,
            List<String> teams,
            String startDateStr,
            List<String> venues,
            List<String> referees,
            String creatorUsername) {

        if (teams == null || teams.size() < 2) {
            return List.of();
        }
        LocalDate startDate;
        try {
            startDate = startDateStr != null && !startDateStr.isBlank()
                    ? LocalDate.parse(startDateStr)
                    : LocalDate.now().plusDays(1);
        } catch (Exception e) {
            startDate = LocalDate.now().plusDays(1);
        }

        List<String> venuePool = (venues != null && !venues.isEmpty())
                ? venues
                : List.of("SVĐ Quốc Gia Mỹ Đình", "SVĐ Hàng Đẫy", "SVĐ Cẩm Phả");
        List<String> refPool = (referees != null && !referees.isEmpty())
                ? referees
                : List.of("TT FIFA Ngô Duy Lân", "TT Hoàng Ngọc Hà", "TT Nguyễn Mạnh Hải");

        List<Match> planned = new ArrayList<>();
        String fmt = format != null ? format.toUpperCase() : "ROUND_ROBIN";

        if ("KNOCKOUT".equals(fmt)) {
            int roundNum = 1;
            String roundLabel = teams.size() >= 8 ? "Tứ Kết" : (teams.size() >= 4 ? "Bán Kết" : "Chung Kết");
            for (int i = 0; i + 1 < teams.size(); i += 2) {
                Match m = new Match();
                m.setTournamentId(tournamentId);
                m.setCompetitionId(competitionId != null ? competitionId : 1L);
                m.setRound(roundLabel + " - Trận " + roundNum);
                m.setGroupName("Nhánh " + ((i / 2) + 1));
                m.setParticipant1(teams.get(i));
                m.setParticipant2(teams.get(i + 1));
                m.setLocation(venuePool.get(i % venuePool.size()));
                m.setReferee(refPool.get(i % refPool.size()));
                m.setMatchTime(startDate.plusDays(i / 2).atTime(17 + ((i % 2) * 2), 0));
                m.setStatus("SCHEDULED");
                m.setOwnerUsername(creatorUsername);
                validateDistinctParticipants(m);
                planned.add(m);
                roundNum++;
            }
        } else {
            int idx = 0;
            for (int i = 0; i < teams.size(); i++) {
                for (int j = i + 1; j < teams.size(); j++) {
                    if ("GROUP_STAGE".equals(fmt) && (i % 2 != j % 2)) {
                        continue;
                    }
                    Match m = new Match();
                    m.setTournamentId(tournamentId);
                    m.setCompetitionId(competitionId != null ? competitionId : 1L);
                    String group = "GROUP_STAGE".equals(fmt) ? (i % 2 == 0 ? "Bảng A" : "Bảng B") : "Vòng Tròn";
                    m.setGroupName(group);
                    m.setRound(group + " - Lượt " + ((idx / 2) + 1));
                    m.setParticipant1(teams.get(i));
                    m.setParticipant2(teams.get(j));
                    m.setLocation(venuePool.get(idx % venuePool.size()));
                    m.setReferee(refPool.get(idx % refPool.size()));
                    m.setMatchTime(startDate.plusDays(idx / 2).atTime(idx % 2 == 0 ? 17 : 19, 30));
                    m.setStatus("SCHEDULED");
                    m.setOwnerUsername(creatorUsername);
                    validateDistinctParticipants(m);
                    planned.add(m);
                    idx++;
                }
            }
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

        return matchRepository.saveAll(planned);
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

            matchEventProducer.publishMatchFinished(event);
            System.out.println("[MATCH FINISHED] Event published for Match ID = " + savedMatch.getId());
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
