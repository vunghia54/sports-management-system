$ErrorActionPreference = "Stop"
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ">>> KHOI DONG HE THONG VA CHAY TEST TOAN DIEN 6 SERVICE <<<" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$jarPath = "target\sports-management-system-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path $jarPath)) {
    Write-Error "Khong tim thay file jar: $jarPath"
    exit 1
}

# Start Spring Boot in background job / process
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = "java"
$psi.Arguments = "-jar $jarPath"
$psi.UseShellExecute = $false
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.CreateNoWindow = $true

$proc = [System.Diagnostics.Process]::Start($psi)

try {
    Write-Host "Dang doi Spring Boot khoi dong tren port 8080..." -ForegroundColor Yellow
    $started = $false
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 1
        try {
            $resp = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/tournaments" -Method Get -TimeoutSec 2
            if ($resp) {
                $started = $true
                break
            }
        } catch {
            # Doi tiep
        }
    }

    if (-not $started) {
        Write-Host "Spring Boot chua san sang sau 30s. Log loi:" -ForegroundColor Red
        Write-Host ($proc.StandardError.ReadToEnd())
        Write-Host ($proc.StandardOutput.ReadToEnd())
        exit 1
    }

    Write-Host "[OK] Spring Boot da chay thanh cong tai port 8080!`n" -ForegroundColor Green

    # TEST 1: TOURNAMENT SERVICE
    Write-Host "--- TEST 1: TOURNAMENT SERVICE ---" -ForegroundColor Magenta
    $tournaments = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/tournaments" -Method Get
    Write-Host "1.1 Danh sach Giai dau (Tournament): $($tournaments.name) tai $($tournaments.location)" -ForegroundColor White

    $sports = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/sports" -Method Get
    Write-Host "1.2 Mon the thao (Sports): $($sports.Count) mon" -ForegroundColor White
    foreach ($s in $sports) {
        Write-Host "   - Mon: $($s.name) | Loai: $($s.sportType) | Mo ta: $($s.description)"
    }

    $competitions = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/competitions" -Method Get
    Write-Host "1.3 Noi dung thi dau (Competitions): $($competitions.Count) noi dung" -ForegroundColor White
    foreach ($c in $competitions) {
        Write-Host "   - Noi dung: $($c.name) (ID: $($c.id)) | Trang thai: $($c.status)"
    }

    # TEST 2: TEAM SERVICE
    Write-Host "`n--- TEST 2: TEAM SERVICE ---" -ForegroundColor Magenta
    $teams = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/teams" -Method Get
    Write-Host "2.1 Danh sach Doi thi dau (Teams): $($teams.Count) doi" -ForegroundColor White
    foreach ($t in $teams) {
        Write-Host "   - Doi: $($t.name) (Ma: $($t.shortName)) | HLV/QL: $($t.managerName) | SDT: $($t.contactPhone)"
    }

    # TEST 3: ATHLETE SERVICE
    Write-Host "`n--- TEST 3: ATHLETE SERVICE ---" -ForegroundColor Magenta
    $athletes = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/athletes" -Method Get
    Write-Host "3.1 Danh sach Van dong vien (Athletes): $($athletes.Count) VDV" -ForegroundColor White
    foreach ($a in $athletes) {
        Write-Host "   - VDV: $($a.fullName) | Gioi tinh: $($a.gender) | CCCD: $($a.identityCardNumber) | Suc khoe: $($a.healthStatus)"
    }

    # TEST 4: REGISTRATION & PARTICIPANT (MATCH MODULE)
    Write-Host "`n--- TEST 4: REGISTRATION & PARTICIPANTS ---" -ForegroundColor Magenta
    $registrations = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/registrations" -Method Get
    Write-Host "4.1 Tong so ho so dang ky: $($registrations.Count) ho so" -ForegroundColor White

    $participants = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/participants/competition/1" -Method Get
    Write-Host "4.2 Danh sach thanh phan tham gia chinh thuc (Participants) noi dung Bong da (Comp ID 1): $($participants.Count) doi" -ForegroundColor White
    foreach ($p in $participants) {
        Write-Host "   - Participant: $($p.displayName) (Type: $($p.participantType), RefID: $($p.referenceId))"
    }

    # TEST 5: MATCH SERVICE
    Write-Host "`n--- TEST 5: MATCH SERVICE ---" -ForegroundColor Magenta
    $matches = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/matches" -Method Get
    Write-Host "5.1 Danh sach tran dau (Matches): $($matches.Count) tran" -ForegroundColor White
    foreach ($m in $matches) {
        Write-Host "   - Match $($m.id): $($m.participant1) vs $($m.participant2) | Vong: $($m.round) | Trang thai: $($m.status) | San: $($m.location)"
    }

    # TEST 6: RESULT SERVICE
    Write-Host "`n--- TEST 6: RESULT SERVICE ---" -ForegroundColor Magenta
    $results = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/results" -Method Get
    Write-Host "6.1 Danh sach ket qua tran dau (Results): $($results.Count) ket qua" -ForegroundColor White

    $res1 = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/results/match/1" -Method Get
    Write-Host "6.2 Chi tiet ket qua Match 1:" -ForegroundColor White
    Write-Host "   - Ti so: $($res1.result.scoreParticipant1) - $($res1.result.scoreParticipant2) | Thang: $($res1.result.winner) | Trang thai: $($res1.result.status)"
    Write-Host "   - Nguoi xac nhan: $($res1.result.confirmedBy) tai $($res1.result.confirmedAt)"
    Write-Host "   - Cac su kien ban thang/the:"
    foreach ($d in $res1.details) {
        Write-Host "     * Phut $($d.detailTime): $($d.detailType) boi $($d.athleteName) ($($d.participantName)) - $($d.detailValue)"
    }

    # TEST 7: RANKING SERVICE
    Write-Host "`n--- TEST 7: RANKING SERVICE ---" -ForegroundColor Magenta
    $rankingResp = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/rankings/competition/1" -Method Get
    Write-Host "7.1 $($rankingResp.ranking.rankingName):" -ForegroundColor Cyan
    Write-Host ("{0,-5} {1,-18} {2,-5} {3,-5} {4,-5} {5,-5} {6,-5} {7,-8} {8,-5}" -f "Hang", "Doi bong", "Tran", "Thang", "Hoa", "Thua", "BT", "HieuSo", "Diem") -ForegroundColor Yellow
    Write-Host "----------------------------------------------------------------------"
    foreach ($item in $rankingResp.items) {
        Write-Host ("{0,-5} {1,-18} {2,-5} {3,-5} {4,-5} {5,-5} {6,-5} {7,-8} {8,-5}" -f `
            $item.rankPosition, $item.participantName, $item.matchesPlayed, $item.won, $item.drawn, $item.lost, $item.goalsFor, $item.goalDifference, $item.points)
    }

    # TEST 8: WORKFLOW LIVE TEST (POST -> RESULT -> CONFIRM -> RECALCULATE)
    Write-Host "`n--- TEST 8: LIVE END-TO-END WORKFLOW TEST ---" -ForegroundColor Magenta
    Write-Host "8.1 Tao them 1 tran dau moi: CNTT FC vs Co Khi FC..." -ForegroundColor White
    $newMatchBody = @{
        tournamentId = 1
        competitionId = 1
        round = "Vòng 2"
        participant1 = "CNTT FC"
        participant2 = "Cơ Khí FC"
        location = "Sân Bách Khoa"
        status = "SCHEDULED"
    } | ConvertTo-Json

    $createdMatch = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/matches" -Method Post -Body $newMatchBody -ContentType "application/json"
    Write-Host "   -> Match duoc tao thanh cong voi ID = $($createdMatch.id)" -ForegroundColor Green

    Write-Host "8.2 Ghi nhan ket qua tran dau moi: CNTT FC thang 3 - 0..." -ForegroundColor White
    $newResultBody = @{
        matchId = $createdMatch.id
        scoreParticipant1 = 3
        scoreParticipant2 = 0
        notes = "CNTT FC thi dau ap dao"
        details = @(
            @{ detailType = "GOAL"; participantName = "CNTT FC"; athleteName = "Nguyễn Văn Long"; detailTime = "10'"; detailValue = "1-0" },
            @{ detailType = "GOAL"; participantName = "CNTT FC"; athleteName = "Nguyễn Văn Long"; detailTime = "55'"; detailValue = "2-0" },
            @{ detailType = "GOAL"; participantName = "CNTT FC"; athleteName = "Nguyễn Văn Long"; detailTime = "80'"; detailValue = "3-0" }
        )
    } | ConvertTo-Json -Depth 5

    $recordedResult = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/results" -Method Post -Body $newResultBody -ContentType "application/json"
    Write-Host "   -> Ket qua ghi nhan ID = $($recordedResult.result.id), status = $($recordedResult.result.status)" -ForegroundColor Green

    Write-Host "8.3 Ban to chuc xac nhan ket qua (CONFIRM)..." -ForegroundColor White
    $confirmBody = @{ confirmedBy = "TRUONG_BAN_TO_CHUC" } | ConvertTo-Json
    $confirmedResult = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/results/$($recordedResult.result.id)/confirm" -Method Put -Body $confirmBody -ContentType "application/json"
    Write-Host "   -> Ket qua da duoc xac nhan: status = $($confirmedResult.result.status), winner = $($confirmedResult.result.winner)" -ForegroundColor Green

    Write-Host "8.4 Kiem tra Bang xep hang tu dong cap nhat sau khi xac nhan:" -ForegroundColor White
    $updatedRanking = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/rankings/competition/1" -Method Get
    Write-Host ("{0,-5} {1,-18} {2,-5} {3,-5} {4,-5} {5,-5} {6,-5} {7,-8} {8,-5}" -f "Hang", "Doi bong", "Tran", "Thang", "Hoa", "Thua", "BT", "HieuSo", "Diem") -ForegroundColor Yellow
    Write-Host "----------------------------------------------------------------------"
    foreach ($item in $updatedRanking.items) {
        Write-Host ("{0,-5} {1,-18} {2,-5} {3,-5} {4,-5} {5,-5} {6,-5} {7,-8} {8,-5}" -f `
            $item.rankPosition, $item.participantName, $item.matchesPlayed, $item.won, $item.drawn, $item.lost, $item.goalsFor, $item.goalDifference, $item.points)
    }

    Write-Host "`n==========================================================" -ForegroundColor Green
    Write-Host ">>> TAT CA 8 PHAN TEST CHO 6 DICH VU DEU THANH CONG 100%! <<<" -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Green
}
finally {
    Write-Host "`nDang tat tien trinh Spring Boot..." -ForegroundColor Gray
    if ($proc -and -not $proc.HasExited) {
        $proc.Kill()
        $proc.WaitForExit(5000)
    }
    Write-Host "Da tat tien trinh thanh cong." -ForegroundColor Gray
}
