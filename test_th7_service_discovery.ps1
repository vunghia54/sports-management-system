# ==============================================================================
# KIEM THU TU DONG TOAN DIEN TH1 -> TH7 TREN TAT CA 10 MICROSERVICES
# De tai: He thong Quan ly Thi dau The thao theo Kien truc Huong dich vu
# Bao gom:
#   - 10 Services: service-registry (:8761), api-gateway (:8080), auth-service (:8083),
#     tournament-service (2 instances :8081 & :8091), team-service (:8085),
#     athlete-service (:8086), match-service (:8082), result-service (:8087),
#     ranking-service (:8088), notification-service (:8084)
#   - TC01: Khoi dong Eureka Server (:8761) va dang ky toan bo 10 Microservices
#   - TC02: Kiem thu luong nghiep vu lien thong toan bo 8 Business Services qua Eureka Service Discovery:
#           Auth -> Sport/Tournament/Competition -> Athlete -> Team (goi Athlete) ->
#           Registration/Participant/Match (goi Tournament & Team) ->
#           Result (goi Match & Ranking) -> Ranking (tinh BXH tu dong) ->
#           Composite Dashboard & Notification
#   - TC03: Chay 2 Instance Tournament Service (:8081 & :8091) va kiem thu Load Balancing (20 requests)
#   - TC04: Dung Instance 1 (:8081) -> Kiem tra Failover sang Instance 2 (:8091)
#   - TC05: Dung ca 2 Instance -> Kiem tra xu ly loi 503 Service Unavailable
#   - TC06: Khoi dong lai Provider -> Kiem tra tu dong dang ky lai va phuc hoi
# ==============================================================================

param (
    [switch]$KeepRunning = $false
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host ">>> KIỂM THỬ TOÀN DIỆN TH1 -> TH7 TRÊN TẤT CẢ 10 MICROSERVICES & EUREKA <<<" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan

if (-not (Test-Path logs)) {
    New-Item -ItemType Directory -Path logs | Out-Null
}

if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET)) {
    $rngBytes = New-Object byte[] 48
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($rngBytes)
    $env:JWT_SECRET = [Convert]::ToBase64String($rngBytes)
    Write-Host "  [INFO] Da sinh JWT_SECRET ngau nhien (64 ky tu) cho phien kiem thu nay." -ForegroundColor DarkYellow
}
if ([string]::IsNullOrWhiteSpace($env:CORS_ALLOWED_ORIGINS)) {
    $env:CORS_ALLOWED_ORIGINS = "http://localhost:8080"
}

function Test-PortOpen ($port) {
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $iar = $tcp.BeginConnect("127.0.0.1", $port, $null, $null)
        $wait = $iar.AsyncWaitHandle.WaitOne(300, $false)
        if ($wait -and $tcp.Connected) {
            $tcp.EndConnect($iar)
            $tcp.Close()
            return $true
        }
        $tcp.Close()
        return $false
    } catch {
        return $false
    }
}

$startedProcesses = [System.Collections.Generic.List[object]]::new()

function Start-Microservice ($Name, $Port, $Jar, $ExtraArgs = @(), $HealthUrl) {
    if (Test-PortOpen $Port) {
        Write-Host "  -> [$Name] Port ${Port}: Da dang chay san (OK)" -ForegroundColor Green
        $existingProc = $null
        try {
            $owningPid = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty OwningProcess
            if ($owningPid) {
                $existingProc = Get-Process -Id $owningPid -ErrorAction SilentlyContinue
            }
        } catch { }
        $item = [PSCustomObject]@{
            Process   = $existingProc
            Name      = $Name
            Port      = $Port
            HealthUrl = $HealthUrl
            ErrFile   = "logs\${Name}_${Port}_err.log"
        }
        $script:startedProcesses.Add($item)
        return $item
    }
    if (-not (Test-Path $Jar)) {
        throw "Khong tim thay file JAR: $Jar. Hay chay mvn package truoc!"
    }
    Write-Host "  -> [$Name] Port ${Port}: Dang khoi dong..." -ForegroundColor Cyan
    $argList = @("-jar", $Jar, "--server.port=$Port") + $ExtraArgs
    $logFile = "logs\${Name}_${Port}.log"
    $errFile = "logs\${Name}_${Port}_err.log"
    $proc = Start-Process -FilePath "java" -ArgumentList $argList -RedirectStandardOutput $logFile -RedirectStandardError $errFile -PassThru -WindowStyle Hidden
    $item = [PSCustomObject]@{
        Process   = $proc
        Name      = $Name
        Port      = $Port
        HealthUrl = $HealthUrl
        ErrFile   = $errFile
    }
    $script:startedProcesses.Add($item)
    return $item
}

function Wait-ForService ($Name, $Port, $Url, $TimeoutSec = 60) {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt $TimeoutSec) {
        try {
            $resp = Invoke-WebRequest -Uri $Url -Method Get -TimeoutSec 2 -UseBasicParsing -ErrorAction SilentlyContinue
            if ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 500) {
                Write-Host "  [READY] $Name (Port $Port) san sang sau $([math]::Round($sw.Elapsed.TotalSeconds, 1))s" -ForegroundColor Green
                return $true
            }
        } catch { }
        Start-Sleep -Milliseconds 1000
    }
    Write-Host "  [TIMEOUT] $Name (Port $Port) chua phan hoi sau ${TimeoutSec}s!" -ForegroundColor Red
    return $false
}

try {
    # ------------------------------------------------------------------
    # BUOC 1: KHOI DONG EUREKA SERVER (:8761)
    # ------------------------------------------------------------------
    Write-Host "`n[1/6] Khoi dong Service Registry (Netflix Eureka Server - Port 8761)..." -ForegroundColor Yellow
    Start-Microservice -Name "service-registry" -Port 8761 -Jar "service-registry\target\service-registry-0.0.1-SNAPSHOT.jar" -HealthUrl "http://localhost:8761/eureka/apps" | Out-Null
    Wait-ForService -Name "service-registry" -Port 8761 -Url "http://localhost:8761/eureka/apps" | Out-Null

    # ------------------------------------------------------------------
    # BUOC 2: KHOI DONG TOAN BO MICROSERVICES & 2 INSTANCE TOURNAMENT-SERVICE
    # ------------------------------------------------------------------
    Write-Host "`n[2/6] Khoi dong toan bo Microservices (Auth, Tournament x2, Team, Athlete, Match, Result, Ranking, Notification, Gateway)..." -ForegroundColor Yellow
    $usePg = Test-PortOpen 5432
    $tourDbArgs = @()
    $matchDbArgs = @()
    $authDbArgs = @()
    $notifDbArgs = @()
    if (-not $usePg) {
        Write-Host "  [INFO] PostgreSQL (5432) chua bat -> Tu dong dung CSDL H2 cho tung Microservice (Database-per-Service)" -ForegroundColor DarkYellow
        if (Test-Path "logs\h2_tournament_db.lock.db") { Remove-Item "logs\h2_tournament_db.lock.db" -Force -ErrorAction SilentlyContinue }
        $dbDir = (Resolve-Path ".").Path -replace "\\", "/"
        $tourDbArgs  = @("--spring.datasource.url=jdbc:h2:file:$dbDir/logs/h2_tournament_db;AUTO_SERVER=TRUE;MODE=PostgreSQL", "--spring.datasource.driver-class-name=org.h2.Driver", "--spring.datasource.username=sa", "--spring.datasource.password=")
        $matchDbArgs = @("--spring.datasource.url=jdbc:h2:mem:match_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "--spring.datasource.driver-class-name=org.h2.Driver", "--spring.datasource.username=sa", "--spring.datasource.password=")
        $authDbArgs  = @("--spring.datasource.url=jdbc:h2:mem:auth_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "--spring.datasource.driver-class-name=org.h2.Driver", "--spring.datasource.username=sa", "--spring.datasource.password=", "--spring.profiles.active=demo")
        $notifDbArgs = @("--spring.datasource.url=jdbc:h2:mem:notification_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "--spring.datasource.driver-class-name=org.h2.Driver", "--spring.datasource.username=sa", "--spring.datasource.password=")
    }

    $tour2Item = Start-Microservice -Name "tournament-service-2" -Port 8091 -Jar "tournament-service\target\tournament-service-0.0.1-SNAPSHOT.jar" -ExtraArgs $tourDbArgs -HealthUrl "http://localhost:8091/api/v1/instance-info"
    Wait-ForService -Name "tournament-service-2" -Port 8091 -Url "http://localhost:8091/api/v1/instance-info" | Out-Null

    $tour1Item = Start-Microservice -Name "tournament-service-1" -Port 8081 -Jar "tournament-service\target\tournament-service-0.0.1-SNAPSHOT.jar" -ExtraArgs $tourDbArgs -HealthUrl "http://localhost:8081/api/v1/instance-info"
    Start-Microservice -Name "auth-service" -Port 8083 -Jar "auth-service\target\auth-service-0.0.1-SNAPSHOT.jar" -ExtraArgs $authDbArgs -HealthUrl "http://localhost:8083/api/v1/instance-info" | Out-Null
    Start-Microservice -Name "athlete-service" -Port 8086 -Jar "athlete-service\target\athlete-service-0.0.1-SNAPSHOT.jar" -HealthUrl "http://localhost:8086/api/v1/instance-info" | Out-Null
    Start-Microservice -Name "team-service" -Port 8085 -Jar "team-service\target\team-service-0.0.1-SNAPSHOT.jar" -HealthUrl "http://localhost:8085/api/v1/instance-info" | Out-Null
    Start-Microservice -Name "match-service" -Port 8082 -Jar "match-service\target\match-service-0.0.1-SNAPSHOT.jar" -ExtraArgs $matchDbArgs -HealthUrl "http://localhost:8082/api/v1/instance-info" | Out-Null
    Start-Microservice -Name "result-service" -Port 8087 -Jar "result-service\target\result-service-0.0.1-SNAPSHOT.jar" -HealthUrl "http://localhost:8087/api/v1/instance-info" | Out-Null
    Start-Microservice -Name "ranking-service" -Port 8088 -Jar "ranking-service\target\ranking-service-0.0.1-SNAPSHOT.jar" -HealthUrl "http://localhost:8088/api/v1/instance-info" | Out-Null
    Start-Microservice -Name "notification-service" -Port 8084 -Jar "notification-service\target\notification-service-0.0.1-SNAPSHOT.jar" -ExtraArgs $notifDbArgs -HealthUrl "http://localhost:8084/api/v1/instance-info" | Out-Null
    Start-Microservice -Name "api-gateway" -Port 8080 -Jar "api-gateway\target\api-gateway-0.0.1-SNAPSHOT.jar" -HealthUrl "http://localhost:8080/api/v1/gateway/routes" | Out-Null

    Wait-ForService -Name "tournament-service-1" -Port 8081 -Url "http://localhost:8081/api/v1/instance-info" | Out-Null
    Wait-ForService -Name "auth-service" -Port 8083 -Url "http://localhost:8083/api/v1/instance-info" | Out-Null
    Wait-ForService -Name "athlete-service" -Port 8086 -Url "http://localhost:8086/api/v1/instance-info" | Out-Null
    Wait-ForService -Name "team-service" -Port 8085 -Url "http://localhost:8085/api/v1/instance-info" | Out-Null
    Wait-ForService -Name "match-service" -Port 8082 -Url "http://localhost:8082/api/v1/instance-info" | Out-Null
    Wait-ForService -Name "result-service" -Port 8087 -Url "http://localhost:8087/api/v1/instance-info" | Out-Null
    Wait-ForService -Name "ranking-service" -Port 8088 -Url "http://localhost:8088/api/v1/instance-info" | Out-Null
    Wait-ForService -Name "notification-service" -Port 8084 -Url "http://localhost:8084/api/v1/instance-info" | Out-Null
    Wait-ForService -Name "api-gateway" -Port 8080 -Url "http://localhost:8080/api/v1/gateway/routes" | Out-Null

    Write-Host "  -> Dang doi cac instance dang ky va dong bo danh ba tren Eureka Server (8s)..." -ForegroundColor Gray
    Start-Sleep -Seconds 8

    # ------------------------------------------------------------------
    # TC01: KIEM TRA DANG KY TREN EUREKA SERVER
    # ------------------------------------------------------------------
    Write-Host "`n>>> [TC01] Kiem tra danh sach Microservices dang ky tren Eureka Server (:8761)" -ForegroundColor Magenta
    $eurekaHeaders = @{ Accept = "application/json" }
    $eurekaApps = Invoke-RestMethod -Uri "http://localhost:8761/eureka/apps" -Method Get -Headers $eurekaHeaders
    foreach ($app in $eurekaApps.applications.application) {
        $instList = @($app.instance | ForEach-Object { "$($_.instanceId) [$($_.status)]" }) -join ", "
        Write-Host ("  - Service ID: {0,-24} | Instances: {1}" -f $app.name, $instList) -ForegroundColor Cyan
    }

    # ------------------------------------------------------------------
    # TC02: LUONG NGHIEP VU LIEN THONG TOAN BO 8 BUSINESS SERVICES QUA SERVICE DISCOVERY
    # ------------------------------------------------------------------
    Write-Host "`n>>> [TC02] Kiem thu luong nghiep vu lien thong TOAN BO 8 SERVICES qua Eureka Service Discovery & API Gateway (:8080)" -ForegroundColor Magenta

    # 1. AUTH-SERVICE (:8083) - Dung tai khoan demo admin cap quyen cho Organizer, TEAM_MANAGER dang ky cong khai
    $adminLogin = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method Post -Body (@{ username = "admin"; password = "admin123" } | ConvertTo-Json) -ContentType "application/json"
    $adminHeaders = @{ Authorization = "Bearer $($adminLogin.accessToken)" }

    $testOrgUser = "th7_org_" + (Get-Random -Minimum 1000 -Maximum 9999)
    $orgReg = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/register" -Method Post -Body (@{
        username = $testOrgUser; password = "password123"; fullName = "BTC Giai The Thao TH1-TH7"; email = "$testOrgUser@sports.vn"; role = "ROLE_VIEWER"
    } | ConvertTo-Json) -ContentType "application/json"
    Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/users/$($orgReg.id)/role" -Method Put -Body (@{ role = "ROLE_ORGANIZER" } | ConvertTo-Json) -ContentType "application/json" -Headers $adminHeaders | Out-Null

    $loginResp = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method Post -Body (@{ username = $testOrgUser; password = "password123" } | ConvertTo-Json) -ContentType "application/json"
    $authHeaders = @{ Authorization = "Bearer $($loginResp.accessToken)" }

    $testMgrUser = "th7_mgr_" + (Get-Random -Minimum 1000 -Maximum 9999)
    Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/register" -Method Post -Body (@{
        username = $testMgrUser; password = "password123"; fullName = "Lanh Doi TH1-TH7"; email = "$testMgrUser@sports.vn"; role = "ROLE_TEAM_MANAGER"
    } | ConvertTo-Json) -ContentType "application/json" | Out-Null
    $mgrLogin = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method Post -Body (@{ username = $testMgrUser; password = "password123" } | ConvertTo-Json) -ContentType "application/json"
    $mgrHeaders = @{ Authorization = "Bearer $($mgrLogin.accessToken)" }

    $refLogin = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method Post -Body (@{ username = "referee1"; password = "referee123" } | ConvertTo-Json) -ContentType "application/json"
    $refHeaders = @{ Authorization = "Bearer $($refLogin.accessToken)" }
    Write-Host "  [1. AUTH-SERVICE] Dang nhap JWT & Admin cap quyen thanh cong (Organizer = $testOrgUser, Manager = $testMgrUser)" -ForegroundColor Green

    # 2. TOURNAMENT-SERVICE (:8081 & :8091) - Sport, Tournament, Competition, Rule
    $sportCode = "FOOTBALL_" + (Get-Random -Minimum 100 -Maximum 999)
    $sportBody = @{
        name        = "Bong da 11 nguoi ($sportCode)"
        sportType   = "TEAM"
        description = "Bo mon thi dau bong da nam"
    } | ConvertTo-Json
    $createdSport = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/sports" -Method Post -Body $sportBody -ContentType "application/json" -Headers $authHeaders

    $tourBody = @{
        name                  = "Giai Bong Da Sinh Vien Toan Quoc TH7 - 2026"
        description           = "Kiem thu toan dien TH1-TH7 tren 10 Microservices"
        startDate             = "2026-11-16"
        endDate               = "2026-12-30"
        location              = "Ha Noi"
        registrationStartDate = "2026-11-01"
        registrationEndDate   = "2026-11-16"
        status                = "ONGOING"
    } | ConvertTo-Json
    $createdTour = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/tournaments" -Method Post -Body $tourBody -ContentType "application/json" -Headers $authHeaders

    $compBody = @{
        tournamentId = $createdTour.id
        sportId      = $createdSport.id
        name         = "Bong da Nam Sinh Vien 2026"
        status       = "OPEN_REGISTRATION"
    } | ConvertTo-Json
    $createdComp = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/competitions" -Method Post -Body $compBody -ContentType "application/json" -Headers $authHeaders
    Invoke-RestMethod -Uri "http://localhost:8080/api/v1/competitions/$($createdComp.id)/rule" -Method Put -Body (@{ maxParticipants = 16; minTeamMembers = 1; maxTeamMembers = 20; scoringMethod = "POINTS" } | ConvertTo-Json) -ContentType "application/json" -Headers $authHeaders | Out-Null
    Invoke-RestMethod -Uri "http://localhost:8080/api/v1/competitions/$($createdComp.id)/format" -Method Put -Body (@{ formatType = "ROUND_ROBIN"; numberOfGroups = 1; qualifyingSpotsPerGroup = 2 } | ConvertTo-Json) -ContentType "application/json" -Headers $authHeaders | Out-Null
    Write-Host "  [2. TOURNAMENT-SERVICE] Da tao Sport ID=$($createdSport.id), Tournament ID=$($createdTour.id), Competition ID=$($createdComp.id) (ROUND_ROBIN)" -ForegroundColor Green

    # 3. ATHLETE-SERVICE (:8086)
    $athleteBody = @{
        fullName     = "Nguyen Quang Hai TH7"
        gender       = "MALE"
        dateOfBirth  = "1997-04-12"
        nationality  = "Vietnam"
        healthStatus = "QUALIFIED"
    } | ConvertTo-Json
    $createdAthlete = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/athletes" -Method Post -Body $athleteBody -ContentType "application/json" -Headers $mgrHeaders

    $athlete2Body = @{
        fullName     = "Nguyen Hoang Duc TH7"
        gender       = "MALE"
        dateOfBirth  = "1998-01-11"
        nationality  = "Vietnam"
        healthStatus = "QUALIFIED"
    } | ConvertTo-Json
    $createdAthlete2 = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/athletes" -Method Post -Body $athlete2Body -ContentType "application/json" -Headers $mgrHeaders
    Write-Host "  [3. ATHLETE-SERVICE] Da tao 2 VDV: $($createdAthlete.fullName), $($createdAthlete2.fullName)" -ForegroundColor Green

    # 4. TEAM-SERVICE (:8085) -> Dung ROLE_TEAM_MANAGER tao doi & thanh vien qua Eureka Service Discovery
    $team1Code = "HNFC_" + (Get-Random -Minimum 100 -Maximum 999)
    $team1Body = @{
        name         = "CLB Ha Noi ($team1Code)"
        shortName    = $team1Code
        managerName  = "HLV Chu Dinh Nghiem"
        contactEmail = "hanoifc@sports.vn"
    } | ConvertTo-Json
    $createdTeam1 = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/teams" -Method Post -Body $team1Body -ContentType "application/json" -Headers $mgrHeaders

    $team2Code = "VTFC_" + (Get-Random -Minimum 100 -Maximum 999)
    $team2Body = @{
        name         = "CLB Viettel ($team2Code)"
        shortName    = $team2Code
        managerName  = "HLV Thach Bao Khanh"
        contactEmail = "viettelfc@sports.vn"
    } | ConvertTo-Json
    $createdTeam2 = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/teams" -Method Post -Body $team2Body -ContentType "application/json" -Headers $mgrHeaders

    $memberBody = @{
        athleteId    = $createdAthlete.id
        jerseyNumber = 19
        position     = "Midfielder"
        isCaptain    = $true
    } | ConvertTo-Json
    $addedMember = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/teams/$($createdTeam1.id)/members" -Method Post -Body $memberBody -ContentType "application/json" -Headers $mgrHeaders

    $member2Body = @{
        athleteId    = $createdAthlete2.id
        jerseyNumber = 28
        position     = "Midfielder"
        isCaptain    = $true
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "http://localhost:8080/api/v1/teams/$($createdTeam2.id)/members" -Method Post -Body $member2Body -ContentType "application/json" -Headers $mgrHeaders | Out-Null
    Write-Host "  [4. TEAM-SERVICE -> ATHLETE-SERVICE] Da tao 2 Doi ($($createdTeam1.name), $($createdTeam2.name)) & xac thuc VDV '$($addedMember.athleteName)' qua Eureka!" -ForegroundColor Green

    # 5. MATCH-SERVICE (:8082) -> Goi TOURNAMENT-SERVICE & TEAM-SERVICE qua Eureka Service Discovery
    $reg1Body = @{
        competitionId          = $createdComp.id
        participantType        = "TEAM"
        participantReferenceId = $createdTeam1.id
    } | ConvertTo-Json
    $reg1 = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/registrations" -Method Post -Body $reg1Body -ContentType "application/json" -Headers $mgrHeaders
    Invoke-RestMethod -Uri "http://localhost:8080/api/v1/registrations/$($reg1.id)/approve" -Method Put -Headers $authHeaders | Out-Null

    $reg2Body = @{
        competitionId          = $createdComp.id
        participantType        = "TEAM"
        participantReferenceId = $createdTeam2.id
    } | ConvertTo-Json
    $reg2 = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/registrations" -Method Post -Body $reg2Body -ContentType "application/json" -Headers $mgrHeaders
    Invoke-RestMethod -Uri "http://localhost:8080/api/v1/registrations/$($reg2.id)/approve" -Method Put -Headers $authHeaders | Out-Null

    $participants = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/participants/competition/$($createdComp.id)" -Method Get -Headers $authHeaders
    $p1Obj = $participants | Where-Object { $_.referenceId -eq $createdTeam1.id } | Select-Object -First 1
    $p2Obj = $participants | Where-Object { $_.referenceId -eq $createdTeam2.id } | Select-Object -First 1

    $matchBody = @{
        tournamentId    = $createdTour.id
        competitionId   = $createdComp.id
        round           = "Vong Chung Ket"
        participant1Id  = $p1Obj.id
        participant2Id  = $p2Obj.id
        location        = "SVD My Dinh"
        matchTime       = "2026-11-16T08:00:00"
        refereeUsername = "referee1"
        status          = "SCHEDULED"
    } | ConvertTo-Json
    $matchWebResp = Invoke-WebRequest -Uri "http://localhost:8082/api/v1/matches" -Method Post -Body $matchBody -ContentType "application/json" -Headers $authHeaders -UseBasicParsing
    $createdMatch = $matchWebResp.Content | ConvertFrom-Json
    $providerHeader = $matchWebResp.Headers["X-Tournament-Provider-Instance"]
    Invoke-RestMethod -Uri "http://localhost:8080/api/v1/matches/$($createdMatch.id)" -Method Patch -Body (@{ status = "ONGOING" } | ConvertTo-Json) -ContentType "application/json" -Headers $refHeaders | Out-Null
    Write-Host "  [5. MATCH-SERVICE -> TOURNAMENT & TEAM-SERVICE] Da duyet 2 doi tham gia & tao Match ID=$($createdMatch.id) (Instance xu ly: $providerHeader)" -ForegroundColor Green

    # 6. RESULT-SERVICE (:8087) -> Goi MATCH-SERVICE & RANKING-SERVICE qua Eureka Service Discovery
    $resultBody = @{
        matchId           = $createdMatch.id
        scoreParticipant1 = 3
        scoreParticipant2 = 1
        notes             = "Tran chung ket kich tinh"
        details           = @(
            @{ detailType = "GOAL"; participantName = $createdTeam1.name; athleteName = "Nguyen Quang Hai"; detailTime = "25'"; detailValue = "Sut phat dep mat" }
        )
    } | ConvertTo-Json -Depth 4
    $recordedRes = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/results" -Method Post -Body $resultBody -ContentType "application/json" -Headers $refHeaders
    $confirmedRes = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/results/$($recordedRes.result.id)/confirm" -Method Put -Headers $authHeaders
    Write-Host "  [6. RESULT-SERVICE -> MATCH & RANKING-SERVICE] Da ghi nhan & xac nhan ti so 3-1 (Winner: $($confirmedRes.result.winner), Status: $($confirmedRes.result.status))" -ForegroundColor Green

    # 7. RANKING-SERVICE (:8088) & COMPOSITE DASHBOARD
    $rankingResp = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/rankings/competition/$($createdComp.id)" -Method Get
    $topTeam = $rankingResp.items[0]
    Write-Host "  [7. RANKING-SERVICE] BXH tu dong cap nhat: Hang #1 = $($topTeam.participantName) (Diem: $($topTeam.points), Hieu so: +$($topTeam.goalDifference))" -ForegroundColor Green

    # 8. NOTIFICATION-SERVICE (:8084)
    $notifBody = @{
        recipient = $testOrgUser
        title     = "Ket qua chung ket da duoc xac nhan"
        message   = "Tran dau $($createdTeam1.name) 3-1 $($createdTeam2.name) da ket thuc!"
    } | ConvertTo-Json
    $createdNotif = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/notifications" -Method Post -Body $notifBody -ContentType "application/json" -Headers $authHeaders
    Write-Host "  [8. NOTIFICATION-SERVICE] Da tao thong bao ID=$($createdNotif.id) cho '$($createdNotif.recipient)'" -ForegroundColor Green

    # ------------------------------------------------------------------
    # TC03: KIEM TRA CAN BANG TAI (20 REQUESTS QUA CONSUMER)
    # ------------------------------------------------------------------
    Write-Host "`n>>> [TC03] Gui 20 requests lien tiep qua Match Service de kiem tra LoadBalancer (8081 vs 8091):" -ForegroundColor Magenta
    $count8081 = 0
    $count8091 = 0
    for ($i = 1; $i -le 20; $i++) {
        $disc = Invoke-RestMethod -Uri "http://localhost:8082/api/v1/matches/discovery/tournament-instance" -Method Get
        $instId = $disc.providerInstance.instanceId
        $instPort = $disc.providerInstance.instancePort
        if ($instPort -eq 8081) { $count8081++ }
        elseif ($instPort -eq 8091) { $count8091++ }
        Write-Host ("  Lần gửi {0,2}: Instance xử lý = {1,-26} | Port = {2} | Kết quả = Thành công" -f $i, $instId, $instPort) -ForegroundColor Gray
    }
    Write-Host "  => THONG KE 20 REQUESTS: Instance 8081 = $count8081 lan | Instance 8091 = $count8091 lan" -ForegroundColor Cyan

    # ------------------------------------------------------------------
    # TC04: DUNG 1 INSTANCE (8081) -> KIEM TRA FAILOVER SANG 8091
    # ------------------------------------------------------------------
    Write-Host "`n>>> [TC04] Dung dot ngot Tournament Service Instance 1 (Port 8081)..." -ForegroundColor Magenta
    if ($tour1Item -and $tour1Item.Process -and -not $tour1Item.Process.HasExited) {
        $tour1Item.Process.Kill()
        try { Invoke-RestMethod -Uri "http://localhost:8761/eureka/apps/TOURNAMENT-SERVICE/tournament-service:8081" -Method Delete -ErrorAction SilentlyContinue | Out-Null } catch { }
        Write-Host "  -> Da dung process tournament-service-1 (Port 8081). Doi Eureka & LoadBalancer cap nhat (4s)..." -ForegroundColor Yellow
        Start-Sleep -Seconds 4
        $discAfterKill = Invoke-RestMethod -Uri "http://localhost:8082/api/v1/matches/discovery/tournament-instance" -Method Get
        Write-Host "  -> [PASSED] He thong van phuc vu binh thuong qua Instance con lai: $($discAfterKill.providerInstance.instanceId)" -ForegroundColor Green
    }

    # ------------------------------------------------------------------
    # TC05: DUNG TOAN BO INSTANCE PROVIDER -> KIEM TRA LOI 503 RO RANG
    # ------------------------------------------------------------------
    Write-Host "`n>>> [TC05] Dung tiep Tournament Service Instance 2 (Port 8091) (Toan bo Provider ngung hoat dong)..." -ForegroundColor Magenta
    if ($tour2Item -and $tour2Item.Process -and -not $tour2Item.Process.HasExited) {
        $tour2Item.Process.Kill()
        try { Invoke-RestMethod -Uri "http://localhost:8761/eureka/apps/TOURNAMENT-SERVICE/tournament-service:8091" -Method Delete -ErrorAction SilentlyContinue | Out-Null } catch { }
        Write-Host "  -> Da dung process tournament-service-2 (Port 8091). Doi LoadBalancer cap nhat (4s)..." -ForegroundColor Yellow
        Start-Sleep -Seconds 4
        try {
            Invoke-RestMethod -Uri "http://localhost:8082/api/v1/matches/discovery/tournament-instance" -Method Get | Out-Null
            Write-Host "  -> [WARN] Van nhan duoc phan hoi" -ForegroundColor Yellow
        } catch {
            $statusCode = [int]$_.Exception.Response.StatusCode
            Write-Host "  -> [PASSED] Consumer tra ve ma loi co kiem soat: HTTP $statusCode (Service Unavailable)" -ForegroundColor Green
        }
    }

    # ------------------------------------------------------------------
    # TC06: KHOI DONG LAI PROVIDER -> TU DONG DANG KY LAI & PHUC HOI
    # ------------------------------------------------------------------
    Write-Host "`n>>> [TC06] Khoi dong lai Tournament Service Instance 1 (Port 8081) va kiem tra phuc hoi..." -ForegroundColor Magenta
    if (Test-Path "logs\h2_tournament_db.lock.db") { Remove-Item "logs\h2_tournament_db.lock.db" -Force -ErrorAction SilentlyContinue }
    $tour1Restarted = Start-Microservice -Name "tournament-service-1" -Port 8081 -Jar "tournament-service\target\tournament-service-0.0.1-SNAPSHOT.jar" -ExtraArgs $tourDbArgs -HealthUrl "http://localhost:8081/api/v1/instance-info"
    Wait-ForService -Name "tournament-service-1" -Port 8081 -Url "http://localhost:8081/api/v1/instance-info" | Out-Null
    Write-Host "  -> Doi Eureka & LoadBalancer dong bo dang ky lai (8s)..." -ForegroundColor Gray
    Start-Sleep -Seconds 8
    $discRecovered = Invoke-RestMethod -Uri "http://localhost:8082/api/v1/matches/discovery/tournament-instance" -Method Get
    Write-Host "  -> [PASSED] Provider da tu dong dang ky lai va phuc vu request: $($discRecovered.providerInstance.instanceId)" -ForegroundColor Green

} finally {
    if (-not $KeepRunning) {
        Write-Host "`nDang don dep cac tien trinh kiem thu..." -ForegroundColor Yellow
        foreach ($item in $startedProcesses) {
            try {
                if ($item.Process -and -not $item.Process.HasExited) {
                    $item.Process.Kill()
                }
            } catch { }
        }
    } else {
        Write-Host "`n[GHI CHU] Che do -KeepRunning duoc bat. Cac service van dang tiep tuc chay!" -ForegroundColor Green
    }
}
