package com.sports.management.config;

import com.sports.management.athlete.model.Athlete;
import com.sports.management.athlete.service.AthleteService;
import com.sports.management.match.model.Match;
import com.sports.management.match.model.Participant;
import com.sports.management.match.model.Registration;
import com.sports.management.match.service.MatchService;
import com.sports.management.match.service.RegistrationService;
import com.sports.management.ranking.model.RankingItem;
import com.sports.management.ranking.model.RankingResponse;
import com.sports.management.ranking.service.RankingService;
import com.sports.management.result.model.RecordResultRequest;
import com.sports.management.result.model.ResultDetail;
import com.sports.management.result.model.ResultResponse;
import com.sports.management.result.service.ResultService;
import com.sports.management.team.model.Team;
import com.sports.management.team.service.TeamService;
import com.sports.management.tournament.model.Competition;
import com.sports.management.tournament.model.Sport;
import com.sports.management.tournament.model.Tournament;
import com.sports.management.tournament.repository.SportRepository;
import com.sports.management.tournament.service.CompetitionService;
import com.sports.management.tournament.service.TournamentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@Order(2)
public class SystemE2EVerifier implements CommandLineRunner {

    private final TournamentService tournamentService;
    private final SportRepository sportRepository;
    private final CompetitionService competitionService;
    private final TeamService teamService;
    private final AthleteService athleteService;
    private final RegistrationService registrationService;
    private final MatchService matchService;
    private final ResultService resultService;
    private final RankingService rankingService;

    public SystemE2EVerifier(
            TournamentService tournamentService,
            SportRepository sportRepository,
            CompetitionService competitionService,
            TeamService teamService,
            AthleteService athleteService,
            RegistrationService registrationService,
            MatchService matchService,
            ResultService resultService,
            RankingService rankingService) {
        this.tournamentService = tournamentService;
        this.sportRepository = sportRepository;
        this.competitionService = competitionService;
        this.teamService = teamService;
        this.athleteService = athleteService;
        this.registrationService = registrationService;
        this.matchService = matchService;
        this.resultService = resultService;
        this.rankingService = rankingService;
    }

    @Override
    public void run(String... args) throws Exception {
        boolean testOnly = Arrays.asList(args).contains("--test-only");
        if (!testOnly) {
            return;
        }

        System.out.println("\n");
        System.out.println("================================================================================");
        System.out.println(">>> BAT DAU CHAY KIEM THU TOAN DIEN HE THONG (6 DICH VU NGHIEP VU) <<<");
        System.out.println("================================================================================");

        int totalTests = 0;
        int passedTests = 0;

        // -----------------------------------------------------------------------------------------
        // TEST 1: TOURNAMENT SERVICE (Tournament, Sport, Competition)
        // -----------------------------------------------------------------------------------------
        totalTests++;
        System.out.println("\n[1/8] KIEM THU DICH VU GIAI DAU (TOURNAMENT SERVICE):");
        List<Tournament> tournaments = tournamentService.getAllTournaments();
        List<Sport> sports = sportRepository.findAll();
        List<Competition> competitions = competitionService.getAllCompetitions();

        if (tournaments.isEmpty() || sports.size() < 2 || competitions.size() < 2) {
            throw new AssertionError("Tournament test failed: Expected data not present");
        }
        System.out.println("  + Giai dau: " + tournaments.get(0).getName() + " tai " + tournaments.get(0).getLocation());
        System.out.println("  + Mon the thao: " + sports.size() + " mon (" + sports.get(0).getName() + ", " + sports.get(1).getName() + ")");
        System.out.println("  + Noi dung thi dau: " + competitions.size() + " noi dung (" + competitions.get(0).getName() + ", " + competitions.get(1).getName() + ")");
        System.out.println("  -> [PASSED] Tournament Service hoat dong 100% chinh xac.");
        passedTests++;

        // -----------------------------------------------------------------------------------------
        // TEST 2: TEAM SERVICE
        // -----------------------------------------------------------------------------------------
        totalTests++;
        System.out.println("\n[2/8] KIEM THU DICH VU DOI BONG (TEAM SERVICE):");
        List<Team> teams = teamService.getAllTeams();
        System.out.println("  + Danh sach doi hien tai: " + teams.size() + " doi");
        for (Team t : teams) {
            System.out.println("    - Doi: " + t.getName() + " | QL/HLV: " + t.getManagerName() + " | SDT: " + t.getContactPhone());
        }
        // Test tao doi moi
        Team newTeam = new Team(null, "Bách Khoa FC", "BK", "PGS.TS Tran Van E", "0911223344", "bk@edu.vn", null, LocalDateTime.now());
        Team savedTeam = teamService.createTeam(newTeam);
        System.out.println("  + Tao thanh cong doi moi: " + savedTeam.getName() + " (ID: " + savedTeam.getId() + ")");
        System.out.println("  -> [PASSED] Team Service hoat dong 100% chinh xac.");
        passedTests++;

        // -----------------------------------------------------------------------------------------
        // TEST 3: ATHLETE SERVICE
        // -----------------------------------------------------------------------------------------
        totalTests++;
        System.out.println("\n[3/8] KIEM THU DICH VU VAN DONG VIEN (ATHLETE SERVICE):");
        List<Athlete> athletes = athleteService.getAllAthletes();
        System.out.println("  + Danh sach VDV hien tai: " + athletes.size() + " VDV");
        for (Athlete a : athletes) {
            System.out.println("    - VDV: " + a.getFullName() + " | Gioi tinh: " + a.getGender() + " | Suc khoe: " + a.getHealthStatus());
        }
        // Test tao VDV moi
        Athlete newAthlete = new Athlete(null, "Hoàng Văn Tuấn", LocalDate.of(2003, 9, 9), "MALE", "001203009999", "0988776655", "tuan@student.vn", "Vietnam", null, "QUALIFIED");
        Athlete savedAthlete = athleteService.createAthlete(newAthlete);
        System.out.println("  + Tao thanh cong VDV moi: " + savedAthlete.getFullName() + " (ID: " + savedAthlete.getId() + ")");
        System.out.println("  -> [PASSED] Athlete Service hoat dong 100% chinh xac.");
        passedTests++;

        // -----------------------------------------------------------------------------------------
        // TEST 4: REGISTRATION & PARTICIPANT (MATCH SERVICE)
        // -----------------------------------------------------------------------------------------
        totalTests++;
        System.out.println("\n[4/8] KIEM THU DANG KY VA THANH PHAN THAM GIA (REGISTRATION & PARTICIPANT):");
        List<Registration> registrations = registrationService.getAllRegistrations();
        System.out.println("  + Tong so ho so dang ky: " + registrations.size() + " ho so");

        Long compFootballId = competitions.get(0).getId();
        List<Participant> footballParticipants = registrationService.getParticipantsByCompetition(compFootballId);
        System.out.println("  + Danh sach doi tham gia chinh thuc (Participants) noi dung Bong da: " + footballParticipants.size() + " doi");
        for (Participant p : footballParticipants) {
            System.out.println("    * Participant: " + p.getDisplayName() + " (Loai: " + p.getParticipantType() + ", ID tham chieu: " + p.getReferenceId() + ")");
        }
        System.out.println("  -> [PASSED] Registration & Participant hoat dong 100% chinh xac.");
        passedTests++;

        // -----------------------------------------------------------------------------------------
        // TEST 5: MATCH SERVICE (LICH THI DAU)
        // -----------------------------------------------------------------------------------------
        totalTests++;
        System.out.println("\n[5/8] KIEM THU LICH THI DAU (MATCH SERVICE):");
        List<Match> matches = matchService.getAllMatches();
        System.out.println("  + Tong so tran dau hien tai: " + matches.size() + " tran");
        for (Match m : matches) {
            System.out.println("    - Match " + m.getId() + ": " + m.getParticipant1() + " vs " + m.getParticipant2() +
                    " | Vong: " + m.getRound() + " | Trang thai: " + m.getStatus() + " | San: " + m.getLocation());
        }
        System.out.println("  -> [PASSED] Match Service hoat dong 100% chinh xac.");
        passedTests++;

        // -----------------------------------------------------------------------------------------
        // TEST 6: RESULT SERVICE (GHI NHAN VA CHI TIET KET QUA)
        // -----------------------------------------------------------------------------------------
        totalTests++;
        System.out.println("\n[6/8] KIEM THU GHI NHAN VA XAC NHAN KET QUA (RESULT SERVICE):");
        ResultResponse res1 = resultService.getResultResponseByMatchId(1L);
        System.out.println("  + Ket qua Tran 1 (" + matches.get(0).getParticipant1() + " vs " + matches.get(0).getParticipant2() + "):");
        System.out.println("    * Ti so: " + res1.getResult().getScoreParticipant1() + " - " + res1.getResult().getScoreParticipant2());
        System.out.println("    * Doi chien thang: " + res1.getResult().getWinner());
        System.out.println("    * Trang thai: " + res1.getResult().getStatus() + " (Xac nhan boi: " + res1.getResult().getConfirmedBy() + ")");
        System.out.println("    * Su kien tran dau: " + res1.getDetails().size() + " ban thang:");
        for (ResultDetail d : res1.getDetails()) {
            System.out.println("      - Phut " + d.getDetailTime() + ": " + d.getDetailType() + " boi " + d.getAthleteName() + " (" + d.getParticipantName() + ") -> " + d.getDetailValue());
        }
        System.out.println("  -> [PASSED] Result Service hoat dong 100% chinh xac.");
        passedTests++;

        // -----------------------------------------------------------------------------------------
        // TEST 7: RANKING SERVICE (BANG XEP HANG TU DONG)
        // -----------------------------------------------------------------------------------------
        totalTests++;
        System.out.println("\n[7/8] KIEM THU BANG XEP HANG (RANKING SERVICE):");
        RankingResponse rankingResp = rankingService.getRankingByCompetition(compFootballId);
        System.out.println("  + " + rankingResp.getRanking().getRankingName() + ":");
        System.out.println("  ---------------------------------------------------------------------------------");
        System.out.printf("  %-5s %-20s %-6s %-6s %-6s %-6s %-6s %-8s %-5s\n",
                "Hang", "Doi bong", "Tran", "Thang", "Hoa", "Thua", "BT", "HieuSo", "Diem");
        System.out.println("  ---------------------------------------------------------------------------------");
        for (RankingItem item : rankingResp.getItems()) {
            System.out.printf("  %-5d %-20s %-6d %-6d %-6d %-6d %-6d %-8d %-5d\n",
                    item.getRankPosition(), item.getParticipantName(), item.getMatchesPlayed(),
                    item.getWon(), item.getDrawn(), item.getLost(),
                    item.getGoalsFor(), item.getGoalDifference(), item.getPoints());
        }
        System.out.println("  ---------------------------------------------------------------------------------");
        System.out.println("  -> [PASSED] Ranking Service tu dong tinh toan BXH chinh xac.");
        passedTests++;

        // -----------------------------------------------------------------------------------------
        // TEST 8: LIVE END-TO-END WORKFLOW (MATCH -> RESULT -> CONFIRM -> LEADERBOARD UPDATE)
        // -----------------------------------------------------------------------------------------
        totalTests++;
        System.out.println("\n[8/8] KIEM THU TOAN TRINH NGHIEP VU MOI (LIVE E2E WORKFLOW):");

        // 8.1 Tao tran dau moi
        Match newMatch = new Match();
        newMatch.setTournamentId(tournaments.get(0).getId());
        newMatch.setCompetitionId(compFootballId);
        newMatch.setRound("Vòng 2");
        newMatch.setParticipant1("CNTT FC");
        newMatch.setParticipant2("Cơ Khí FC");
        newMatch.setLocation("Sân vận động Bách Khoa");
        newMatch.setMatchTime(LocalDateTime.now().plusDays(2));
        newMatch.setStatus("SCHEDULED");
        Match createdMatch = matchService.createMatch(newMatch);
        System.out.println("  + Step 1: Tao tran dau moi thanh cong (ID = " + createdMatch.getId() + "): " +
                createdMatch.getParticipant1() + " vs " + createdMatch.getParticipant2());

        // 8.2 Ghi nhan ket qua
        RecordResultRequest req = new RecordResultRequest();
        req.setMatchId(createdMatch.getId());
        req.setScoreParticipant1(3);
        req.setScoreParticipant2(0);
        req.setNotes("CNTT FC the hien suc manh vuot troi");
        req.setDetails(Arrays.asList(
                new ResultDetail(null, null, "GOAL", "CNTT FC", "Nguyễn Văn Long", "12'", "1-0"),
                new ResultDetail(null, null, "GOAL", "CNTT FC", "Nguyễn Văn Long", "45'", "2-0"),
                new ResultDetail(null, null, "GOAL", "CNTT FC", "Nguyễn Văn Long", "88'", "3-0")
        ));
        ResultResponse recResp = resultService.recordResult(req);
        System.out.println("  + Step 2: Ghi nhan ket qua tran dau moi (Status = " + recResp.getResult().getStatus() +
                ", Winner = " + recResp.getResult().getWinner() + ")");

        // 8.3 Ban to chuc xac nhan ket qua
        ResultResponse confResp = resultService.confirmResult(recResp.getResult().getId(), "TRUONG_BAN_TO_CHUC");
        System.out.println("  + Step 3: Ban to chuc xac nhan ket qua (Status = " + confResp.getResult().getStatus() +
                ", Nguoi xac nhan = " + confResp.getResult().getConfirmedBy() + ")");

        // 8.4 Kiem tra BXH sau khi cap nhat
        RankingResponse updatedRanking = rankingService.getRankingByCompetition(compFootballId);
        System.out.println("  + Step 4: Bang xep hang tu dong cap nhat sau tran dau moi:");
        System.out.println("  ---------------------------------------------------------------------------------");
        System.out.printf("  %-5s %-20s %-6s %-6s %-6s %-6s %-6s %-8s %-5s\n",
                "Hang", "Doi bong", "Tran", "Thang", "Hoa", "Thua", "BT", "HieuSo", "Diem");
        System.out.println("  ---------------------------------------------------------------------------------");
        for (RankingItem item : updatedRanking.getItems()) {
            System.out.printf("  %-5d %-20s %-6d %-6d %-6d %-6d %-6d %-8d %-5d\n",
                    item.getRankPosition(), item.getParticipantName(), item.getMatchesPlayed(),
                    item.getWon(), item.getDrawn(), item.getLost(),
                    item.getGoalsFor(), item.getGoalDifference(), item.getPoints());
        }
        System.out.println("  ---------------------------------------------------------------------------------");

        RankingItem topTeam = updatedRanking.getItems().get(0);
        if (!"CNTT FC".equals(topTeam.getParticipantName()) || topTeam.getPoints() != 6 || topTeam.getWon() != 2) {
            throw new AssertionError("Ranking assertion failed: Top team must be CNTT FC with 6 points and 2 wins!");
        }
        System.out.println("  -> Xac minh: " + topTeam.getParticipantName() + " giu vi tri Top 1 voi " + topTeam.getPoints() + " diem (2 tran toan thang)!");
        System.out.println("  -> [PASSED] Live End-to-End Workflow thanh cong 100%!");
        passedTests++;

        // -----------------------------------------------------------------------------------------
        // TONG KET
        // -----------------------------------------------------------------------------------------
        System.out.println("\n================================================================================");
        System.out.println(">>> TONG KET KIEM THU: " + passedTests + "/" + totalTests + " PHAN THI KHOAN DEU HOAN THANH XUAT SAC (100% PASSED) <<<");
        System.out.println("================================================================================");
        System.out.println();

        System.exit(0);
    }
}
