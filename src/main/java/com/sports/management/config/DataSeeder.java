package com.sports.management.config;

import com.sports.management.athlete.model.Athlete;
import com.sports.management.athlete.repository.AthleteRepository;
import com.sports.management.match.model.Match;
import com.sports.management.match.model.Registration;
import com.sports.management.match.repository.MatchRepository;
import com.sports.management.match.service.RegistrationService;
import com.sports.management.ranking.service.RankingService;
import com.sports.management.result.model.RecordResultRequest;
import com.sports.management.result.model.ResultDetail;
import com.sports.management.result.model.ResultResponse;
import com.sports.management.result.service.ResultService;
import com.sports.management.team.model.Team;
import com.sports.management.team.repository.TeamRepository;
import com.sports.management.tournament.model.Competition;
import com.sports.management.tournament.model.Sport;
import com.sports.management.tournament.model.Tournament;
import com.sports.management.tournament.repository.CompetitionRepository;
import com.sports.management.tournament.repository.SportRepository;
import com.sports.management.tournament.repository.TournamentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@org.springframework.core.annotation.Order(1)
public class DataSeeder implements CommandLineRunner {

    private final TournamentRepository tournamentRepository;
    private final SportRepository sportRepository;
    private final CompetitionRepository competitionRepository;
    private final TeamRepository teamRepository;
    private final AthleteRepository athleteRepository;
    private final RegistrationService registrationService;
    private final MatchRepository matchRepository;
    private final ResultService resultService;
    private final RankingService rankingService;

    public DataSeeder(
            TournamentRepository tournamentRepository,
            SportRepository sportRepository,
            CompetitionRepository competitionRepository,
            TeamRepository teamRepository,
            AthleteRepository athleteRepository,
            RegistrationService registrationService,
            MatchRepository matchRepository,
            ResultService resultService,
            RankingService rankingService) {
        this.tournamentRepository = tournamentRepository;
        this.sportRepository = sportRepository;
        this.competitionRepository = competitionRepository;
        this.teamRepository = teamRepository;
        this.athleteRepository = athleteRepository;
        this.registrationService = registrationService;
        this.matchRepository = matchRepository;
        this.resultService = resultService;
        this.rankingService = rankingService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (tournamentRepository.count() > 0) {
            return;
        }

        System.out.println("=================================================");
        System.out.println(">>> SEEDING INITIAL DATA FOR SPORTS SYSTEM <<<");
        System.out.println("=================================================");

        // 1. TAO GIAI DAU (TOURNAMENT)
        Tournament tournament = new Tournament();
        tournament.setName("Hội thao sinh viên 2026");
        tournament.setDescription("Hội thao thể thao sinh viên toàn thành phố năm 2026");
        tournament.setLocation("Hà Nội");
        tournament.setStartDate(LocalDate.of(2026, 11, 1));
        tournament.setEndDate(LocalDate.of(2026, 11, 20));
        tournament.setRegistrationStartDate(LocalDate.of(2026, 10, 1));
        tournament.setRegistrationEndDate(LocalDate.of(2026, 10, 31));
        tournament.setStatus("ACTIVE");
        Tournament savedTournament = tournamentRepository.save(tournament);

        // 2. TAO MON THI DAU (SPORT)
        Sport football = new Sport(null, "Bóng đá", "TEAM", "Môn thể thao vua");
        Sport badminton = new Sport(null, "Cầu lông", "ATHLETE", "Môn thể thao vợt đối kháng");
        sportRepository.saveAll(Arrays.asList(football, badminton));

        // 3. TAO NOI DUNG THI DAU (COMPETITION)
        Competition compFootball = new Competition(
                null,
                savedTournament.getId(),
                football.getId(),
                "Bóng đá nam sinh viên",
                "ACTIVE"
        );
        Competition compBadminton = new Competition(
                null,
                savedTournament.getId(),
                badminton.getId(),
                "Cầu lông đơn nam",
                "ACTIVE"
        );
        competitionRepository.saveAll(Arrays.asList(compFootball, compBadminton));

        // 4. TAO DOI THI DAU (TEAM)
        Team team1 = new Team(null, "CNTT FC", "CNTT", "Nguyễn Văn A", "0912345678", "contact@cntt.edu.vn", null, LocalDateTime.now());
        Team team2 = new Team(null, "Kinh Tế FC", "KT", "Trần Thị B", "0912345679", "contact@kinhte.edu.vn", null, LocalDateTime.now());
        Team team3 = new Team(null, "Cơ Khí FC", "CK", "Lê Văn C", "0912345680", "contact@cokhi.edu.vn", null, LocalDateTime.now());
        Team team4 = new Team(null, "Ngoại Ngữ FC", "NN", "Phạm Văn D", "0912345681", "contact@nn.edu.vn", null, LocalDateTime.now());
        teamRepository.saveAll(Arrays.asList(team1, team2, team3, team4));

        // 5. TAO VAN DONG VIEN (ATHLETE)
        Athlete a1 = new Athlete(null, "Nguyễn Văn Long", LocalDate.of(2003, 5, 10), "MALE", "001203001234", "0987654321", "long@student.vn", "Vietnam", null, "QUALIFIED");
        Athlete a2 = new Athlete(null, "Trần Đức Anh", LocalDate.of(2004, 8, 15), "MALE", "001204001235", "0987654322", "anh@student.vn", "Vietnam", null, "QUALIFIED");
        Athlete a3 = new Athlete(null, "Lê Hoàng Nam", LocalDate.of(2003, 12, 20), "MALE", "001203001236", "0987654323", "nam@student.vn", "Vietnam", null, "QUALIFIED");
        Athlete a4 = new Athlete(null, "Phạm Minh Quân", LocalDate.of(2004, 3, 5), "MALE", "001204001237", "0987654324", "quan@student.vn", "Vietnam", null, "QUALIFIED");
        athleteRepository.saveAll(Arrays.asList(a1, a2, a3, a4));

        // 6. DANG KY VA DUYET THI DAU (REGISTRATION & APPROVAL)
        Registration reg1 = registrationService.register(new Registration(null, compFootball.getId(), "TEAM", team1.getId(), team1.getTeamName(), null, null, null));
        Registration reg2 = registrationService.register(new Registration(null, compFootball.getId(), "TEAM", team2.getId(), team2.getTeamName(), null, null, null));
        Registration reg3 = registrationService.register(new Registration(null, compFootball.getId(), "TEAM", team3.getId(), team3.getTeamName(), null, null, null));
        Registration reg4 = registrationService.register(new Registration(null, compFootball.getId(), "TEAM", team4.getId(), team4.getTeamName(), null, null, null));

        registrationService.approveRegistration(reg1.getId());
        registrationService.approveRegistration(reg2.getId());
        registrationService.approveRegistration(reg3.getId());
        registrationService.approveRegistration(reg4.getId());

        // Dang ky cau long don nam
        Registration regBadminton1 = registrationService.register(new Registration(null, compBadminton.getId(), "ATHLETE", a1.getId(), a1.getFullName(), null, null, null));
        Registration regBadminton2 = registrationService.register(new Registration(null, compBadminton.getId(), "ATHLETE", a2.getId(), a2.getFullName(), null, null, null));
        registrationService.approveRegistration(regBadminton1.getId());
        registrationService.approveRegistration(regBadminton2.getId());

        // 7. LICH THI DAU (MATCHES)
        Match match1 = new Match();
        match1.setTournamentId(savedTournament.getId());
        match1.setCompetitionId(compFootball.getId());
        match1.setRound("Vòng 1");
        match1.setParticipant1("CNTT FC");
        match1.setParticipant2("Kinh Tế FC");
        match1.setLocation("Sân vận động Bách Khoa");
        match1.setMatchTime(LocalDateTime.now().minusDays(1));
        match1.setStatus("SCHEDULED");
        match1 = matchRepository.save(match1);

        Match match2 = new Match();
        match2.setTournamentId(savedTournament.getId());
        match2.setCompetitionId(compFootball.getId());
        match2.setRound("Vòng 1");
        match2.setParticipant1("Cơ Khí FC");
        match2.setParticipant2("Ngoại Ngữ FC");
        match2.setLocation("Sân vận động Thủy Lợi");
        match2.setMatchTime(LocalDateTime.now().minusDays(1));
        match2.setStatus("SCHEDULED");
        match2 = matchRepository.save(match2);

        Match match3 = new Match();
        match3.setTournamentId(savedTournament.getId());
        match3.setCompetitionId(compBadminton.getId());
        match3.setRound("Tứ kết");
        match3.setParticipant1("Nguyễn Văn Long");
        match3.setParticipant2("Trần Đức Anh");
        match3.setLocation("Nhà thi đấu Cầu Giấy");
        match3.setMatchTime(LocalDateTime.now().plusDays(1));
        match3.setStatus("SCHEDULED");
        matchRepository.save(match3);

        // 8. GHI NHAN VA XAC NHAN KET QUA (RESULT)
        // Tran 1: CNTT FC 2 - 1 Kinh Te FC
        RecordResultRequest req1 = new RecordResultRequest();
        req1.setMatchId(match1.getId());
        req1.setScoreParticipant1(2);
        req1.setScoreParticipant2(1);
        req1.setNotes("Trận đấu diễn ra sôi nổi và hấp dẫn");
        req1.setDetails(Arrays.asList(
                new ResultDetail(null, null, "GOAL", "CNTT FC", "Nguyễn Văn Long", "15'", "1-0"),
                new ResultDetail(null, null, "GOAL", "Kinh Tế FC", "Trần Đức Anh", "35'", "1-1"),
                new ResultDetail(null, null, "GOAL", "CNTT FC", "Nguyễn Văn Long", "78'", "2-1")
        ));
        ResultResponse resResp1 = resultService.recordResult(req1);
        resultService.confirmResult(resResp1.getResult().getId(), "ORGANIZER_TRUONG");

        // Tran 2: Co Khi FC 1 - 1 Ngoai Ngu FC (Hoa)
        RecordResultRequest req2 = new RecordResultRequest();
        req2.setMatchId(match2.getId());
        req2.setScoreParticipant1(1);
        req2.setScoreParticipant2(1);
        req2.setNotes("Trận hòa căng thẳng");
        req2.setDetails(Arrays.asList(
                new ResultDetail(null, null, "GOAL", "Cơ Khí FC", "Lê Hoàng Nam", "22'", "1-0"),
                new ResultDetail(null, null, "GOAL", "Ngoại Ngữ FC", "Phạm Minh Quân", "60'", "1-1")
        ));
        ResultResponse resResp2 = resultService.recordResult(req2);
        resultService.confirmResult(resResp2.getResult().getId(), "ORGANIZER_TRUONG");

        // 9. CAP NHAT BANG XEP HANG (RANKINGS)
        rankingService.recalculateRanking(compFootball.getId());

        System.out.println("=================================================");
        System.out.println(">>> SEEDING COMPLETED SUCCESSFULLY! <<<");
        System.out.println("=================================================");
    }
}
