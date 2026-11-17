param(
    [string]$ComposeProjectName = "sports-management-system",
    [string]$EnvFile = ".env",
    [string]$ResultsDir = "docs/test/test-results",
    [switch]$SkipBuild = $false,
    [switch]$KeepRunning = $false
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

if (-not (Test-Path $EnvFile)) {
    throw "Khong tim thay file env '$EnvFile'. Hay tao tu .env.example (Copy-Item .env.example .env)."
}

New-Item -ItemType Directory -Force -Path $ResultsDir | Out-Null

# Doc bien tu EnvFile de lay dung cong publish & thong tin ket noi DB (khong ghi secret ra bao cao)
$envMap = @{}
foreach ($line in Get-Content $EnvFile) {
    $trimmed = $line.Trim()
    if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith("#")) { continue }
    $idx = $trimmed.IndexOf("=")
    if ($idx -gt 0) {
        $k = $trimmed.Substring(0, $idx).Trim()
        $v = $trimmed.Substring($idx + 1).Trim()
        $envMap[$k] = $v
    }
}

function Get-Cfg([string]$Key, [string]$DefaultVal) {
    if ($envMap.ContainsKey($Key) -and -not [string]::IsNullOrWhiteSpace($envMap[$Key])) {
        return $envMap[$Key]
    }
    return $DefaultVal
}

$registryPort     = Get-Cfg "REGISTRY_PORT" "8761"
$gatewayPort      = Get-Cfg "GATEWAY_PORT" "8080"
$authPort         = Get-Cfg "AUTH_PORT" "8083"
$tour1Port        = Get-Cfg "TOURNAMENT_PORT" "8081"
$tour2Port        = Get-Cfg "TOURNAMENT_2_PORT" "8091"
$matchPort        = Get-Cfg "MATCH_PORT" "8082"
$notifPort        = Get-Cfg "NOTIFICATION_PORT" "8084"
$teamPort         = Get-Cfg "TEAM_PORT" "8085"
$athletePort      = Get-Cfg "ATHLETE_PORT" "8086"
$resultPort       = Get-Cfg "RESULT_PORT" "8087"
$rankingPort      = Get-Cfg "RANKING_PORT" "8088"
$rabbitMgmtPort   = Get-Cfg "RABBITMQ_MGMT_PORT" "15672"
$rabbitUser       = Get-Cfg "RABBITMQ_USERNAME" "guest"
$rabbitPass       = Get-Cfg "RABBITMQ_PASSWORD" "guest"
$dbUser           = Get-Cfg "DB_USERNAME" "sports"
$demoSeedUsers    = (Get-Cfg "APP_DEMO_SEED_USERS" "false").ToLowerInvariant()

function Invoke-Compose([string[]]$ComposeArgs) {
    $baseArgs = @("compose", "-p", $ComposeProjectName, "--env-file", $EnvFile) + $ComposeArgs
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $out = & docker @baseArgs 2>&1
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $prevEap
    }
    if ($exitCode -ne 0) {
        $textOut = ($out | ForEach-Object { $_.ToString() }) -join "`n"
        throw "Lenh 'docker $($baseArgs -join ' ')' that bai (exit=$exitCode):`n$textOut"
    }
    return (($out | ForEach-Object { $_.ToString() }) -join "`n")
}

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) {
        throw "ASSERTION FAILED: $Message"
    }
}

function Invoke-HttpJson([string]$Method, [string]$Url, $Body = $null, [hashtable]$Headers = @{}, [int]$TimeoutSec = 10) {
    $params = @{
        Method             = $Method
        Uri                = $Url
        TimeoutSec         = $TimeoutSec
        Headers            = $Headers
        UseBasicParsing    = $true
    }
    if ($null -ne $Body) {
        $params["Body"] = ($Body | ConvertTo-Json -Depth 8)
        $params["ContentType"] = "application/json; charset=utf-8"
    }
    try {
        $resp = Invoke-WebRequest @params
        $rawText = if ($resp.Content -is [byte[]]) {
            [System.Text.Encoding]::UTF8.GetString($resp.Content)
        } elseif ($null -ne $resp.RawContentStream) {
            [System.Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
        } else {
            [string]$resp.Content
        }
        $json = $null
        if (-not [string]::IsNullOrWhiteSpace($rawText)) {
            try { $json = $rawText | ConvertFrom-Json } catch { $json = $rawText }
        }
        return [PSCustomObject]@{ StatusCode = [int]$resp.StatusCode; Body = $json; Raw = $rawText; Headers = $resp.Headers }
    } catch [System.Net.WebException] {
        $webResp = $_.Exception.Response
        $code = if ($webResp) { [int]$webResp.StatusCode } else { 0 }
        $raw = ""
        if ($webResp) {
            $reader = New-Object System.IO.StreamReader($webResp.GetResponseStream(), [System.Text.Encoding]::UTF8)
            $raw = $reader.ReadToEnd()
            $reader.Close()
        }
        $json = $null
        if (-not [string]::IsNullOrWhiteSpace($raw)) {
            try { $json = $raw | ConvertFrom-Json } catch { $json = $raw }
        }
        return [PSCustomObject]@{ StatusCode = $code; Body = $json; Raw = $raw; Headers = if ($webResp) { $webResp.Headers } else { $null } }
    } catch {
        return [PSCustomObject]@{ StatusCode = 0; Body = $null; Raw = $_.Exception.Message; Headers = $null }
    }
}

function Wait-ComposeServicesHealthy([int]$TimeoutSec = 180) {
    $expected = @(
        "postgres-db","rabbitmq","service-registry","api-gateway","auth-service",
        "tournament-service","tournament-service-2","team-service","athlete-service",
        "match-service","result-service","ranking-service","notification-service"
    )
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt $TimeoutSec) {
        $psJsonRaw = & docker compose -p $ComposeProjectName --env-file $EnvFile ps --format json 2>$null
        if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($psJsonRaw)) {
            $rows = @()
            foreach ($line in ($psJsonRaw -split "`r?`n")) {
                if (-not [string]::IsNullOrWhiteSpace($line)) {
                    $rows += ($line | ConvertFrom-Json)
                }
            }
            $allReady = $true
            foreach ($svc in $expected) {
                $match = $rows | Where-Object { $_.Service -eq $svc } | Select-Object -First 1
                if ($null -eq $match -or $match.State -ne "running" -or $match.Health -ne "healthy") {
                    $allReady = $false
                    break
                }
            }
            if ($allReady) {
                Write-Host "  [READY] 13/13 container healthy sau $([math]::Round($sw.Elapsed.TotalSeconds, 1))s" -ForegroundColor Green
                return
            }
        }
        Start-Sleep -Seconds 3
    }
    $debugPs = Invoke-Compose @("ps")
    throw "Timeout (${TimeoutSec}s) cho 13 container healthy:`n$debugPs"
}

function Wait-EurekaInstances([hashtable]$RequiredCounts, [int]$TimeoutSec = 90) {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt $TimeoutSec) {
        try {
            $apps = Invoke-RestMethod -Uri "http://127.0.0.1:$registryPort/eureka/apps" -Headers @{ Accept = "application/json" } -TimeoutSec 5
            $map = @{}
            foreach ($app in @($apps.applications.application)) {
                if ($null -eq $app) { continue }
                $upCount = @($app.instance | Where-Object { $_.status -eq "UP" }).Count
                $map[$app.name.ToUpperInvariant()] = $upCount
            }
            $ok = $true
            foreach ($k in $RequiredCounts.Keys) {
                if (-not $map.ContainsKey($k) -or $map[$k] -lt $RequiredCounts[$k]) {
                    $ok = $false
                    break
                }
            }
            if ($ok) {
                Write-Host "  [EUREKA] Tat ca dich vu da dang ky UP tren Eureka sau $([math]::Round($sw.Elapsed.TotalSeconds, 1))s" -ForegroundColor Green
                return $apps
            }
        } catch { }
        Start-Sleep -Seconds 3
    }
    throw "Timeout (${TimeoutSec}s) cho Eureka dang ky du cac instance yeu cau."
}

function Wait-EurekaCountExact([string]$AppName, [int]$ExpectedCount, [int]$TimeoutSec = 60) {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt $TimeoutSec) {
        try {
            $apps = Invoke-RestMethod -Uri "http://127.0.0.1:$registryPort/eureka/apps" -Headers @{ Accept = "application/json" } -TimeoutSec 5
            $app = @($apps.applications.application) | Where-Object { $_.name.ToUpperInvariant() -eq $AppName.ToUpperInvariant() } | Select-Object -First 1
            $upCount = if ($app) { @($app.instance | Where-Object { $_.status -eq "UP" }).Count } else { 0 }
            if ($upCount -eq $ExpectedCount) {
                return $upCount
            }
        } catch {
            if ($ExpectedCount -eq 0) {
                # 404/no app yet
            }
        }
        Start-Sleep -Seconds 2
    }
    throw "Timeout (${TimeoutSec}s) cho Eureka cap nhat $AppName == $ExpectedCount instance UP."
}

function Render-TerminalPng([string]$Text, [string]$OutPngPath, [string]$Title) {
    $pyCode = @"
import sys
from PIL import Image, ImageDraw, ImageFont

title, text_path, out_png = sys.argv[1], sys.argv[2], sys.argv[3]
with open(text_path, 'r', encoding='utf-8') as f:
    lines = [l.rstrip('\n') for l in f.readlines()]

try:
    font = ImageFont.truetype('consola.ttf', 15)
    title_font = ImageFont.truetype('seguisb.ttf', 18)
except Exception:
    font = ImageFont.load_default()
    title_font = font

width = max(980, max((len(l) for l in lines), default=40) * 9 + 60)
height = max(260, len(lines) * 22 + 80)
img = Image.new('RGB', (width, height), (18, 22, 30))
draw = ImageDraw.Draw(img)
draw.rectangle([(0, 0), (width, 44)], fill=(30, 41, 59))
draw.text((18, 12), title, fill=(226, 232, 240), font=title_font)
y = 58
for line in lines:
    draw.text((18, y), line, fill=(167, 243, 208), font=font)
    y += 22
img.save(out_png)
"@
    $tmpTxt = [System.IO.Path]::GetTempFileName()
    $tmpPy = [System.IO.Path]::GetTempFileName() + ".py"
    [System.IO.File]::WriteAllText($tmpTxt, $Text, [System.Text.Encoding]::UTF8)
    [System.IO.File]::WriteAllText($tmpPy, $pyCode, [System.Text.Encoding]::UTF8)
    & python $tmpPy $Title $tmpTxt $OutPngPath
    Remove-Item $tmpTxt, $tmpPy -Force -ErrorAction SilentlyContinue
}

function Capture-EurekaScreenshot([string]$OutPngPath) {
    $browser = @("C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe", "C:\Program Files\Google\Chrome\Application\chrome.exe") | Where-Object { Test-Path $_ } | Select-Object -First 1
    if (-not $browser) { return $false }
    $absOut = [System.IO.Path]::GetFullPath($OutPngPath)
    $tmpProfile = Join-Path $env:TEMP ("th8_shot_" + [guid]::NewGuid().ToString("N"))
    New-Item -ItemType Directory -Force -Path $tmpProfile | Out-Null
    try {
        $argsList = @("--headless=new", "--disable-gpu", "--no-sandbox", "--user-data-dir=$tmpProfile", "--window-size=1440,1200", "--virtual-time-budget=3000", "--screenshot=$absOut", "http://127.0.0.1:$registryPort/")
        $proc = Start-Process -FilePath $browser -ArgumentList $argsList -Wait -PassThru -WindowStyle Hidden
        return (($proc.ExitCode -eq 0) -and (Test-Path $absOut))
    } finally {
        Remove-Item $tmpProfile -Recurse -Force -ErrorAction SilentlyContinue
    }
}

$summary = [ordered]@{}
$baseGw = "http://127.0.0.1:$gatewayPort"

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "TH8 DEPLOYMENT VERIFICATION (Project=$ComposeProjectName, Env=$EnvFile)" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan

# ------------------------------------------------------------------------------
# DEP01: Build & Start + Container status + Eureka Registry
# ------------------------------------------------------------------------------
Write-Host "`n[DEP01] Khoi dong toan he thong va kiem tra container + Eureka..." -ForegroundColor Yellow
if ($SkipBuild) {
    Invoke-Compose @("up", "-d") | Out-Null
} else {
    Invoke-Compose @("up", "-d", "--build") | Out-Null
}
Wait-ComposeServicesHealthy -TimeoutSec 240

$requiredEureka = @{
    "API-GATEWAY"          = 1
    "AUTH-SERVICE"         = 1
    "TOURNAMENT-SERVICE"   = 2
    "TEAM-SERVICE"         = 1
    "ATHLETE-SERVICE"      = 1
    "MATCH-SERVICE"        = 1
    "RESULT-SERVICE"       = 1
    "RANKING-SERVICE"      = 1
    "NOTIFICATION-SERVICE" = 1
}
$eurekaData = Wait-EurekaInstances -RequiredCounts $requiredEureka -TimeoutSec 90
$psText = Invoke-Compose @("ps")
Set-Content -Path (Join-Path $ResultsDir "dep01-compose-ps.txt") -Value $psText -Encoding UTF8
$eurekaSummary = @()
foreach ($app in @($eurekaData.applications.application)) {
    $eurekaSummary += [ordered]@{
        name      = $app.name
        instances = @($app.instance | ForEach-Object { @{ instanceId = $_.instanceId; status = $_.status; hostName = $_.hostName; port = $_.port.'$' } })
    }
}
$eurekaSummary | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $ResultsDir "dep01-eureka-apps.json") -Encoding UTF8
Render-TerminalPng -Text $psText -OutPngPath (Join-Path $ResultsDir "dep01-compose-ps.png") -Title "docker compose -p $ComposeProjectName ps"
$shotOk = Capture-EurekaScreenshot -OutPngPath (Join-Path $ResultsDir "dep01-eureka-dashboard.png")
Write-Host "  [DEP01] PASS (Eureka screenshot captured: $shotOk)" -ForegroundColor Green
$summary["DEP01"] = "PASS"

# ------------------------------------------------------------------------------
# DEP02: Actuator /actuator/health & /actuator/info across ALL containers
# ------------------------------------------------------------------------------
Write-Host "`n[DEP02] Kiem tra /actuator/health va an toan thong tin..." -ForegroundColor Yellow
$endpoints = @(
    @{ service = "service-registry"; url = "http://127.0.0.1:$registryPort" },
    @{ service = "api-gateway"; url = "http://127.0.0.1:$gatewayPort" },
    @{ service = "auth-service"; url = "http://127.0.0.1:$authPort" },
    @{ service = "tournament-service-1"; url = "http://127.0.0.1:$tour1Port" },
    @{ service = "tournament-service-2"; url = "http://127.0.0.1:$tour2Port" },
    @{ service = "team-service"; url = "http://127.0.0.1:$teamPort" },
    @{ service = "athlete-service"; url = "http://127.0.0.1:$athletePort" },
    @{ service = "match-service"; url = "http://127.0.0.1:$matchPort" },
    @{ service = "result-service"; url = "http://127.0.0.1:$resultPort" },
    @{ service = "ranking-service"; url = "http://127.0.0.1:$rankingPort" },
    @{ service = "notification-service"; url = "http://127.0.0.1:$notifPort" }
)
$dep02Results = @()
foreach ($ep in $endpoints) {
    $h = Invoke-HttpJson "GET" "$($ep.url)/actuator/health"
    Assert-True ($h.StatusCode -eq 200) "$($ep.service) /actuator/health tra status $($h.StatusCode)"
    Assert-True ($h.Body.status -eq "UP") "$($ep.service) health status != UP: $($h.Raw)"
    Assert-True ($null -eq $h.Body.components) "$($ep.service) lo components khi chua bat show-details"
    $prom = Invoke-HttpJson "GET" "$($ep.url)/actuator/prometheus"
    Assert-True ($prom.StatusCode -eq 200 -and $prom.Raw -match "jvm_") "$($ep.service) /actuator/prometheus khong hop le (status=$($prom.StatusCode))"
    $envCheck = Invoke-HttpJson "GET" "$($ep.url)/actuator/env"
    Assert-True ($envCheck.StatusCode -in @(401, 403, 404)) "$($ep.service) /actuator/env khong duoc mo cong khai (code=$($envCheck.StatusCode))"
    $dep02Results += [ordered]@{
        service = $ep.service
        healthCode = $h.StatusCode
        healthBody = $h.Body
        prometheusCode = $prom.StatusCode
        envEndpointStatus = $envCheck.StatusCode
    }
}
$dep02Results | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $ResultsDir "dep02-actuator-health.json") -Encoding UTF8
Write-Host "  [DEP02] PASS (11/11 health UP + prometheus 200, show-details=never, /actuator/env bi chan)" -ForegroundColor Green
$summary["DEP02"] = "PASS"

# ------------------------------------------------------------------------------
# DEP03: Stop all tournament-service instances -> verify consumer 503 + logs
# ------------------------------------------------------------------------------
Write-Host "`n[DEP03] Dung toan bo tournament-service va kiem tra loi 503 + log..." -ForegroundColor Yellow
$dep03StartUtc = (Get-Date).AddSeconds(-2).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
Invoke-Compose @("stop", "tournament-service", "tournament-service-2") | Out-Null
Wait-EurekaCountExact -AppName "TOURNAMENT-SERVICE" -ExpectedCount 0 -TimeoutSec 45 | Out-Null
Start-Sleep -Seconds 5  # Cho LoadBalancer TTL (3s) het han tu nhien

$discDown = Invoke-HttpJson "GET" "http://127.0.0.1:$matchPort/api/v1/matches/discovery/tournament-instance"
Assert-True ($discDown.StatusCode -eq 503) "Ky vong match-service tra 503 khi tournament-service dung het, thuc te: $($discDown.StatusCode) / $($discDown.Raw)"
$gwDown = Invoke-HttpJson "GET" "$baseGw/api/v1/tournaments"
Assert-True ($gwDown.StatusCode -eq 503) "Ky vong api-gateway tra 503 khi tournament-service dung het, thuc te: $($gwDown.StatusCode)"

$matchLogs = Invoke-Compose @("logs", "--since", $dep03StartUtc, "match-service")
Set-Content -Path (Join-Path $ResultsDir "dep03-match-service.log") -Value $matchLogs -Encoding UTF8
Assert-True ($matchLogs -match "Provider http://tournament-service discovery failed|Tournament Service") "Log match-service khong ghi nhan nguyen nhan loi provider unavailable"
Assert-True (-not ($matchLogs -match "Bearer eyJ")) "Log khong duoc chua JWT token!"

[ordered]@{
    timestampUtc = $dep03StartUtc
    matchDiscoveryStatus = $discDown.StatusCode
    matchDiscoveryMessage = $discDown.Body.message
    gatewayStatus = $gwDown.StatusCode
    gatewayMessage = $gwDown.Body.message
} | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $ResultsDir "dep03-provider-down.json") -Encoding UTF8
Write-Host "  [DEP03] PASS (Consumer tra 503 + log co thoi diem, ten service va nguyen nhan)" -ForegroundColor Green
$summary["DEP03"] = "PASS"

# ------------------------------------------------------------------------------
# DEP04: Start provider instances back -> wait Eureka -> verify recovery
# ------------------------------------------------------------------------------
Write-Host "`n[DEP04] Khoi dong lai tournament-service va cho Eureka phuc hoi..." -ForegroundColor Yellow
Invoke-Compose @("start", "tournament-service", "tournament-service-2") | Out-Null
Wait-ComposeServicesHealthy -TimeoutSec 120
Wait-EurekaCountExact -AppName "TOURNAMENT-SERVICE" -ExpectedCount 2 -TimeoutSec 60 | Out-Null
Start-Sleep -Seconds 4  # Cho LoadBalancer cache TTL 3s cap nhat

$discUp = Invoke-HttpJson "GET" "http://127.0.0.1:$matchPort/api/v1/matches/discovery/tournament-instance"
Assert-True ($discUp.StatusCode -eq 200) "Sau khi khoi dong lai provider, discovery tra $($discUp.StatusCode): $($discUp.Raw)"
[ordered]@{
    statusCode = $discUp.StatusCode
    handledBy = $discUp.Body.providerInstance.instanceId
} | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $ResultsDir "dep04-recovery.json") -Encoding UTF8
Write-Host "  [DEP04] PASS (Phuc hoi qua Eureka: handledBy=$($discUp.Body.providerInstance.instanceId))" -ForegroundColor Green
$summary["DEP04"] = "PASS"

# Helper lay token Organizer qua API Gateway (khong ghi secret ra file)
$suffix = Get-Random -Minimum 10000 -Maximum 99999
$testOrgUser = "th8_org_$suffix"
$testMgrUser = "th8_mgr_$suffix"
$testRefUser = "th8_ref_$suffix"
$testPw = "PassTh8!2026"

if ($demoSeedUsers -eq "true") {
    $admLogin = Invoke-HttpJson "POST" "$baseGw/api/v1/auth/login" @{ username = "admin"; password = (Get-Cfg "DEMO_ADMIN_PW" "admin123") }
    Assert-True ($admLogin.StatusCode -eq 200) "Demo admin login that bai (status=$($admLogin.StatusCode))"
    $admHdr = @{ Authorization = "Bearer $($admLogin.Body.accessToken)" }

    $orgReg = Invoke-HttpJson "POST" "$baseGw/api/v1/auth/register" @{ username = $testOrgUser; password = $testPw; fullName = "BTC Kiem Thu TH8"; email = "$testOrgUser@test.local"; role = "ROLE_VIEWER" }
    Assert-True ($orgReg.StatusCode -eq 201) "Dang ky organizer that bai: $($orgReg.Raw)"
    $promOrg = Invoke-HttpJson "PUT" "$baseGw/api/v1/auth/users/$($orgReg.Body.id)/role" @{ role = "ROLE_ORGANIZER" } $admHdr
    Assert-True ($promOrg.StatusCode -eq 200) "Cap quyen ROLE_ORGANIZER that bai: $($promOrg.Raw)"

    $refReg = Invoke-HttpJson "POST" "$baseGw/api/v1/auth/register" @{ username = $testRefUser; password = $testPw; fullName = "Trong Tai TH8"; email = "$testRefUser@test.local"; role = "ROLE_VIEWER" }
    Assert-True ($refReg.StatusCode -eq 201) "Dang ky referee that bai: $($refReg.Raw)"
    $promRef = Invoke-HttpJson "PUT" "$baseGw/api/v1/auth/users/$($refReg.Body.id)/role" @{ role = "ROLE_REFEREE" } $admHdr
    Assert-True ($promRef.StatusCode -eq 200) "Cap quyen ROLE_REFEREE that bai: $($promRef.Raw)"
} else {
    throw "Hay bat APP_DEMO_SEED_USERS=true trong EnvFile kiem thu de script tu tao tai khoan organizer/referee kiem thu nghiep vu."
}

$orgLogin = Invoke-HttpJson "POST" "$baseGw/api/v1/auth/login" @{ username = $testOrgUser; password = $testPw }
Assert-True ($orgLogin.StatusCode -eq 200) "Login organizer that bai"
$orgHdr = @{ Authorization = "Bearer $($orgLogin.Body.accessToken)" }

$mgrReg = Invoke-HttpJson "POST" "$baseGw/api/v1/auth/register" @{ username = $testMgrUser; password = $testPw; fullName = "Lanh Doi TH8"; email = "$testMgrUser@test.local"; role = "ROLE_TEAM_MANAGER" }
Assert-True ($mgrReg.StatusCode -eq 201) "Dang ky team manager that bai"
$mgrLogin = Invoke-HttpJson "POST" "$baseGw/api/v1/auth/login" @{ username = $testMgrUser; password = $testPw }
Assert-True ($mgrLogin.StatusCode -eq 200) "Login team manager that bai"
$mgrHdr = @{ Authorization = "Bearer $($mgrLogin.Body.accessToken)" }

$refLogin = Invoke-HttpJson "POST" "$baseGw/api/v1/auth/login" @{ username = $testRefUser; password = $testPw }
Assert-True ($refLogin.StatusCode -eq 200) "Login referee that bai"
$refHdr = @{ Authorization = "Bearer $($refLogin.Body.accessToken)" }

# ------------------------------------------------------------------------------
# DEP05: Create test data, restart PostgreSQL + services, assert data persists
# ------------------------------------------------------------------------------
Write-Host "`n[DEP05] Tao du lieu nghiep vu, restart PostgreSQL & service, kiem tra khong mat..." -ForegroundColor Yellow
$sportName = "Bong da TH8 $suffix"
$spRes = Invoke-HttpJson "POST" "$baseGw/api/v1/sports" @{ name = $sportName; sportType = "TEAM"; description = "TH8 Persistence Check" } $orgHdr
Assert-True ($spRes.StatusCode -eq 201) "Tao Sport that bai: $($spRes.Raw)"
$sportId = $spRes.Body.id

$todayDate    = Get-Date
$todayStr     = $todayDate.ToString("yyyy-MM-dd")
$regStartStr  = $todayDate.AddDays(-1).ToString("yyyy-MM-dd")
$tourEndStr   = $todayDate.AddDays(30).ToString("yyyy-MM-dd")

$tourName = "Giai Dau Ben Vung TH8 $suffix"
$trRes = Invoke-HttpJson "POST" "$baseGw/api/v1/tournaments" @{
    name = $tourName; description = "Kiem tra volume PostgreSQL"; startDate = $todayStr; endDate = $tourEndStr
    location = "Ha Noi"; registrationStartDate = $regStartStr; registrationEndDate = $todayStr; status = "ONGOING"
} $orgHdr
Assert-True ($trRes.StatusCode -eq 201) "Tao Tournament that bai (status=$($trRes.StatusCode)): $($trRes.Raw)"
$tourId = $trRes.Body.id

$pgCountBefore = (Invoke-Compose @("exec", "-T", "postgres-db", "psql", "-U", $dbUser, "-d", "tournament_db", "-tAc", "SELECT COUNT(*) FROM tournaments WHERE id = $tourId;")).Trim()
Assert-True ($pgCountBefore -eq "1") "Khong tim thay tournament id=$tourId trong PostgreSQL truoc khi restart (count=$pgCountBefore)"

Invoke-Compose @("restart", "postgres-db", "tournament-service", "tournament-service-2", "auth-service") | Out-Null
Wait-ComposeServicesHealthy -TimeoutSec 120
Wait-EurekaCountExact -AppName "TOURNAMENT-SERVICE" -ExpectedCount 2 -TimeoutSec 60 | Out-Null
Start-Sleep -Seconds 4

$trAfterRestart = Invoke-HttpJson "GET" "$baseGw/api/v1/tournaments/$tourId"
Assert-True ($trAfterRestart.StatusCode -eq 200 -and $trAfterRestart.Body.name -eq $tourName) "Du lieu tournament bi mat hoac sai sau restart PostgreSQL!"
$pgCountAfterRestart = (Invoke-Compose @("exec", "-T", "postgres-db", "psql", "-U", $dbUser, "-d", "tournament_db", "-tAc", "SELECT COUNT(*) FROM tournaments WHERE id = $tourId;")).Trim()
Assert-True ($pgCountAfterRestart -eq "1") "Row count trong PostgreSQL sau restart khac 1: $pgCountAfterRestart"

[ordered]@{
    tournamentId = $tourId
    tournamentName = $trAfterRestart.Body.name
    pgCountBefore = $pgCountBefore
    pgCountAfterRestart = $pgCountAfterRestart
} | ConvertTo-Json -Depth 4 | Set-Content -Path (Join-Path $ResultsDir "dep05-persistence-restart.json") -Encoding UTF8
Write-Host "  [DEP05] PASS (Tournament ID=$tourId con nguyen sau restart DB & service)" -ForegroundColor Green
$summary["DEP05"] = "PASS"

# ------------------------------------------------------------------------------
# DEP06: Full Compose down & up WITHOUT -v -> verify cluster, DB & RabbitMQ data
# ------------------------------------------------------------------------------
Write-Host "`n[DEP06] Dung va khoi dong lai toan bo cum bang docker compose down / up -d (khong -v)..." -ForegroundColor Yellow
$rabbitPair = [Convert]::ToBase64String([System.Text.Encoding]::ASCII.GetBytes("${rabbitUser}:${rabbitPass}"))
$rabbitHdr = @{ Authorization = "Basic $rabbitPair" }
$testQueue = "th8.persistence.test.queue"
$rabbitPayload = "th8-rabbitmq-durable-$suffix"
$nodeNameBefore = (Invoke-Compose @("exec", "-T", "rabbitmq", "sh", "-c", "hostname")).Trim()

$qDecl = Invoke-HttpJson "PUT" "http://127.0.0.1:$rabbitMgmtPort/api/queues/%2F/$testQueue" @{ durable = $true; auto_delete = $false } $rabbitHdr
Assert-True ($qDecl.StatusCode -in @(201, 204)) "Tao durable queue '$testQueue' that bai (status=$($qDecl.StatusCode)): $($qDecl.Raw)"

# Xoa sach message ton du neu co trong queue test rieng truoc khi gui moi
$null = Invoke-HttpJson "DELETE" "http://127.0.0.1:$rabbitMgmtPort/api/queues/%2F/$testQueue/contents" $null $rabbitHdr

$pubRes = Invoke-HttpJson "POST" "http://127.0.0.1:$rabbitMgmtPort/api/exchanges/%2F/amq.default/publish" @{
    properties = @{ delivery_mode = 2 }
    routing_key = $testQueue
    payload = $rabbitPayload
    payload_encoding = "string"
} $rabbitHdr
Assert-True ($pubRes.StatusCode -eq 200 -and $pubRes.Body.routed -eq $true) "Publish persistent message vao '$testQueue' that bai (status=$($pubRes.StatusCode)): $($pubRes.Raw)"
$qBeforeLines = Invoke-Compose @("exec", "-T", "rabbitmq", "rabbitmqctl", "list_queues", "-q", "name", "durable", "messages")

Invoke-Compose @("down") | Out-Null
Invoke-Compose @("up", "-d") | Out-Null
Wait-ComposeServicesHealthy -TimeoutSec 180
Wait-EurekaInstances -RequiredCounts $requiredEureka -TimeoutSec 90 | Out-Null
Start-Sleep -Seconds 4

$nodeNameAfter = (Invoke-Compose @("exec", "-T", "rabbitmq", "sh", "-c", "hostname")).Trim()
Assert-True ($nodeNameBefore -eq "rabbitmq" -and $nodeNameAfter -eq "rabbitmq") "RabbitMQ hostname phai co dinh la 'rabbitmq' (before=$nodeNameBefore, after=$nodeNameAfter)"

$getRes = Invoke-HttpJson "POST" "http://127.0.0.1:$rabbitMgmtPort/api/queues/%2F/$testQueue/get" @{
    count = 5
    ackmode = "ack_requeue_false"
    encoding = "auto"
} $rabbitHdr
Assert-True ($getRes.StatusCode -eq 200) "Doc message tu '$testQueue' sau down/up that bai (status=$($getRes.StatusCode)): $($getRes.Raw)"
$msgsAfter = @($getRes.Body)
$matchedMsg = $msgsAfter | Where-Object { $_.payload -eq $rabbitPayload } | Select-Object -First 1
Assert-True ($null -ne $matchedMsg) "Khong tim thay persistent message '$rabbitPayload' trong queue '$testQueue' sau compose down/up!"
Assert-True ([int]$matchedMsg.properties.delivery_mode -eq 2) "delivery_mode cua message phai bang 2 (persistent), thuc te=$($matchedMsg.properties.delivery_mode)"

$delQueue = Invoke-HttpJson "DELETE" "http://127.0.0.1:$rabbitMgmtPort/api/queues/%2F/$testQueue" $null $rabbitHdr
Assert-True ($delQueue.StatusCode -in @(204, 200)) "Don dep queue kiem thu '$testQueue' that bai (status=$($delQueue.StatusCode))"

[ordered]@{
    nodeHostnameBefore = $nodeNameBefore
    nodeHostnameAfter = $nodeNameAfter
    durableQueue = $testQueue
    queueStatusBeforeDown = $qBeforeLines.Trim()
    payloadSent = $rabbitPayload
    payloadReceivedAfterDownUp = $matchedMsg.payload
    deliveryMode = [int]$matchedMsg.properties.delivery_mode
    redelivered = [bool]$matchedMsg.redelivered
} | ConvertTo-Json -Depth 4 | Set-Content -Path (Join-Path $ResultsDir "dep06-rabbitmq-persistence.json") -Encoding UTF8

$trAfterDownUp = Invoke-HttpJson "GET" "$baseGw/api/v1/tournaments/$tourId"
Assert-True ($trAfterDownUp.StatusCode -eq 200 -and $trAfterDownUp.Body.name -eq $tourName) "Du lieu tournament khong con sau docker compose down/up!"
$reLogin = Invoke-HttpJson "POST" "$baseGw/api/v1/auth/login" @{ username = $testOrgUser; password = $testPw }
Assert-True ($reLogin.StatusCode -eq 200) "Tai khoan organizer tao truoc down/up khong con trong auth_db!"
$orgHdr = @{ Authorization = "Bearer $($reLogin.Body.accessToken)" }

[ordered]@{
    tournamentId = $tourId
    tournamentNameAfterDownUp = $trAfterDownUp.Body.name
    organizerLoginAfterDownUp = $reLogin.StatusCode
    rabbitMqHostname = $nodeNameAfter
    rabbitMqPersistentMessageRecovered = $true
} | ConvertTo-Json -Depth 4 | Set-Content -Path (Join-Path $ResultsDir "dep06-compose-down-up.json") -Encoding UTF8
Write-Host "  [DEP06] PASS (He thong + PostgreSQL + RabbitMQ persistent message phuc hoi sau docker compose down/up)" -ForegroundColor Green
$summary["DEP06"] = "PASS"

# ------------------------------------------------------------------------------
# SPORT01: Load balancing across 2 tournament-service instances & single failover
# ------------------------------------------------------------------------------
Write-Host "`n[SPORT01] Kiem thu can bang tai 2 tournament instance & failover 1 instance..." -ForegroundColor Yellow
$lbCounts = @{}
$lbRecords = @()
for ($i = 1; $i -le 20; $i++) {
    $r = Invoke-HttpJson "GET" "http://127.0.0.1:$matchPort/api/v1/matches/discovery/tournament-instance"
    Assert-True ($r.StatusCode -eq 200) "LB request #$i failed ($($r.StatusCode))"
    $inst = $r.Body.providerInstance.instanceId
    if (-not $lbCounts.ContainsKey($inst)) { $lbCounts[$inst] = 0 }
    $lbCounts[$inst] = $lbCounts[$inst] + 1
    $lbRecords += "$i -> $inst"
}
Assert-True ($lbCounts.Keys.Count -ge 2) "Ca 2 instance tournament-service phai cung nhan request, thuc te: $($lbCounts | ConvertTo-Json -Compress)"

Invoke-Compose @("stop", "tournament-service") | Out-Null
Wait-EurekaCountExact -AppName "TOURNAMENT-SERVICE" -ExpectedCount 1 -TimeoutSec 45 | Out-Null
Start-Sleep -Seconds 4

$failoverInstances = @()
for ($i = 1; $i -le 10; $i++) {
    $r = Invoke-HttpJson "GET" "http://127.0.0.1:$matchPort/api/v1/matches/discovery/tournament-instance"
    Assert-True ($r.StatusCode -eq 200) "Failover request #$i bi loi khi chi dung instance 1 ($($r.StatusCode))"
    $failoverInstances += $r.Body.providerInstance.instanceId
}
Assert-True (@($failoverInstances | Where-Object { $_ -ne "tournament-service:8091" }).Count -eq 0) "Khi dung instance 1, moi request phai qua tournament-service:8091"

Invoke-Compose @("start", "tournament-service") | Out-Null
Wait-ComposeServicesHealthy -TimeoutSec 120
Wait-EurekaCountExact -AppName "TOURNAMENT-SERVICE" -ExpectedCount 2 -TimeoutSec 60 | Out-Null
Start-Sleep -Seconds 4

[ordered]@{
    loadBalance20Requests = $lbCounts
    sampleSequence = $lbRecords
    failover10RequestsAfterStoppingInstance1 = $failoverInstances
} | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $ResultsDir "sport01-loadbalancing-failover.json") -Encoding UTF8
Write-Host "  [SPORT01] PASS (20 req LB: $($lbCounts | ConvertTo-Json -Compress); failover 10/10 -> tournament-service:8091)" -ForegroundColor Green
$summary["SPORT01"] = "PASS"

# ------------------------------------------------------------------------------
# SPORT02: Full domain flow through API Gateway + PostgreSQL + RabbitMQ
# ------------------------------------------------------------------------------
Write-Host "`n[SPORT02] Kiem thu luong nghiep vu the thao end-to-end + RabbitMQ qua Gateway..." -ForegroundColor Yellow
$sport02StartUtc = (Get-Date).AddSeconds(-2).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")

$compRes = Invoke-HttpJson "POST" "$baseGw/api/v1/competitions" @{
    tournamentId = $tourId; sportId = $sportId; name = "Noi dung Bo mon Nam $suffix"; status = "OPEN_REGISTRATION"
} $orgHdr
Assert-True ($compRes.StatusCode -eq 201) "Tao competition that bai: $($compRes.Raw)"
$compId = $compRes.Body.id

$ruleRes = Invoke-HttpJson "PUT" "$baseGw/api/v1/competitions/$compId/rule" @{
    maxParticipants = 8; minTeamMembers = 1; maxTeamMembers = 15; scoringMethod = "POINTS"
} $orgHdr
Assert-True ($ruleRes.StatusCode -eq 200) "Cap nhat rule that bai: $($ruleRes.Raw)"

$fmtRes = Invoke-HttpJson "PUT" "$baseGw/api/v1/competitions/$compId/format" @{
    formatType = "ROUND_ROBIN"; numberOfGroups = 1; qualifyingSpotsPerGroup = 2
} $orgHdr
Assert-True ($fmtRes.StatusCode -eq 200) "Cap nhat format that bai: $($fmtRes.Raw)"

$team1 = Invoke-HttpJson "POST" "$baseGw/api/v1/teams" @{ name = "CLB Thang Long $suffix"; shortName = "TL$($suffix % 1000)"; coach = "HLV A"; city = "Ha Noi" } $mgrHdr
$team2 = Invoke-HttpJson "POST" "$baseGw/api/v1/teams" @{ name = "CLB Sai Gon $suffix"; shortName = "SG$($suffix % 1000)"; coach = "HLV B"; city = "TP HCM" } $mgrHdr
Assert-True ($team1.StatusCode -eq 201 -and $team2.StatusCode -eq 201) "Tao 2 doi that bai"

$ath1 = Invoke-HttpJson "POST" "$baseGw/api/v1/athletes" @{ fullName = "Van Dong Vien A $suffix"; gender = "MALE"; dateOfBirth = "2000-01-15"; identityCardNumber = ("00100" + $suffix + "11"); healthStatus = "QUALIFIED" } $mgrHdr
$ath2 = Invoke-HttpJson "POST" "$baseGw/api/v1/athletes" @{ fullName = "Van Dong Vien B $suffix"; gender = "MALE"; dateOfBirth = "2001-03-20"; identityCardNumber = ("00100" + $suffix + "22"); healthStatus = "QUALIFIED" } $mgrHdr
Assert-True ($ath1.StatusCode -eq 201 -and $ath2.StatusCode -eq 201) "Tao 2 VDV that bai"

$m1 = Invoke-HttpJson "POST" "$baseGw/api/v1/teams/$($team1.Body.id)/members" @{ athleteId = $ath1.Body.id; jerseyNumber = 9; position = "Tien dao"; role = "CAPTAIN" } $mgrHdr
$m2 = Invoke-HttpJson "POST" "$baseGw/api/v1/teams/$($team2.Body.id)/members" @{ athleteId = $ath2.Body.id; jerseyNumber = 10; position = "Tien ve"; role = "CAPTAIN" } $mgrHdr
Assert-True ($m1.StatusCode -eq 201 -and $m2.StatusCode -eq 201) "Them thanh vien vao doi that bai"

$reg1 = Invoke-HttpJson "POST" "$baseGw/api/v1/registrations" @{ competitionId = $compId; participantType = "TEAM"; participantReferenceId = $team1.Body.id } $mgrHdr
$reg2 = Invoke-HttpJson "POST" "$baseGw/api/v1/registrations" @{ competitionId = $compId; participantType = "TEAM"; participantReferenceId = $team2.Body.id } $mgrHdr
Assert-True ($reg1.StatusCode -eq 201 -and $reg2.StatusCode -eq 201) "Gui dang ky that bai"
Assert-True ($reg1.Body.status -eq "PENDING" -and $reg2.Body.status -eq "PENDING") "Trang thai dang ky ban dau phai la PENDING"

$app1 = Invoke-HttpJson "PUT" "$baseGw/api/v1/registrations/$($reg1.Body.id)/approve" $null $orgHdr
$app2 = Invoke-HttpJson "PUT" "$baseGw/api/v1/registrations/$($reg2.Body.id)/approve" $null $orgHdr
Assert-True ($app1.StatusCode -eq 200 -and $app1.Body.status -eq "APPROVED") "Duyet dang ky 1 that bai"
Assert-True ($app2.StatusCode -eq 200 -and $app2.Body.status -eq "APPROVED") "Duyet dang ky 2 that bai"

$sched = Invoke-HttpJson "POST" "$baseGw/api/v1/matches/generate-schedule" @{
    tournamentId = $tourId; competitionId = $compId; startDate = $todayStr; venues = @("San TH8 $suffix"); referees = @($testRefUser)
} $orgHdr
Assert-True ($sched.StatusCode -eq 201 -and $sched.Body.generatedCount -ge 1) "Tao lich thi dau that bai (status=$($sched.StatusCode)): $($sched.Raw)"
$matchId = $sched.Body.matches[0].id
$matchObj = $sched.Body.matches[0]

# Trong tai bat dau tran -> ONGOING (match_time phai khong o tuong lai qua xa)
$startMatch = Invoke-HttpJson "PUT" "$baseGw/api/v1/matches/$matchId" @{
    tournamentId = $tourId; competitionId = $compId; round = $matchObj.round; groupName = $matchObj.groupName
    matchTime = (Get-Date).AddMinutes(-10).ToString("yyyy-MM-ddTHH:mm:ss")
    location = $matchObj.location; referee = $matchObj.referee; refereeUsername = $testRefUser
    participant1Id = $matchObj.participant1Id; participant2Id = $matchObj.participant2Id
    participant1 = $matchObj.participant1; participant2 = $matchObj.participant2; status = "ONGOING"
} $orgHdr
Assert-True ($startMatch.StatusCode -eq 200 -and $startMatch.Body.status -eq "ONGOING") "Chuyen tran sang ONGOING that bai (status=$($startMatch.StatusCode)): $($startMatch.Raw)"

# Ghi ket qua (PENDING_CONFIRMATION) -> Xac nhan (CONFIRMED -> FINISHED + BXH + RabbitMQ)
$draftRes = Invoke-HttpJson "POST" "$baseGw/api/v1/results" @{
    matchId = $matchId; scoreParticipant1 = 3; scoreParticipant2 = 1; summary = "Tran dau ket thuc 3-1"
} $refHdr
$draftResultObj = if ($draftRes.Body.result) { $draftRes.Body.result } else { $draftRes.Body }
Assert-True ($draftRes.StatusCode -eq 201 -and $draftResultObj.status -eq "PENDING_CONFIRMATION") "Ghi nhap ket qua that bai (status=$($draftRes.StatusCode)): $($draftRes.Raw)"
$resultId = $draftResultObj.id

$confirmRes = Invoke-HttpJson "PUT" "$baseGw/api/v1/results/$resultId/confirm" $null $orgHdr
$confirmResultObj = if ($confirmRes.Body.result) { $confirmRes.Body.result } else { $confirmRes.Body }
Assert-True ($confirmRes.StatusCode -in @(200, 202) -and $confirmResultObj.status -eq "CONFIRMED") "Xac nhan ket qua that bai (status=$($confirmRes.StatusCode)): $($confirmRes.Raw)"

# Cho dong bo match FINISHED, ranking, va notification consumer tu RabbitMQ
$matchAfter = $null
$rankingsAfter = $null
$notifFound = $null
$sw = [System.Diagnostics.Stopwatch]::StartNew()
while ($sw.Elapsed.TotalSeconds -lt 35) {
    $mCheck = Invoke-HttpJson "GET" "$baseGw/api/v1/matches/$matchId"
    $rCheck = Invoke-HttpJson "GET" "$baseGw/api/v1/rankings/competition/$compId"
    $nCheck = Invoke-HttpJson "GET" "$baseGw/api/v1/notifications?recipient=ALL_USERS"
    if ($mCheck.StatusCode -eq 200) { $matchAfter = $mCheck.Body }
    if ($rCheck.StatusCode -eq 200 -and $null -ne $rCheck.Body -and $null -ne $rCheck.Body.items) {
        $items = @($rCheck.Body.items)
        if ($items.Count -ge 2 -and (@($items | Where-Object { $_.matchesPlayed -eq 1 }).Count -ge 2)) {
            $rankingsAfter = $rCheck.Body
        }
    }
    if ($nCheck.StatusCode -eq 200) {
        $notifFound = @($nCheck.Body) | Where-Object { $_.type -eq "MATCH_FINISHED" -and [long]$_.referenceId -eq [long]$matchId } | Select-Object -First 1
    }
    if ($null -ne $matchAfter -and $matchAfter.status -eq "FINISHED" -and $null -ne $rankingsAfter -and $null -ne $notifFound) { break }
    Start-Sleep -Seconds 2
}

Assert-True ($null -ne $matchAfter -and $matchAfter.status -eq "FINISHED") "Match status sau confirm phai la FINISHED, thuc te: $($matchAfter.status)"
Assert-True ($null -ne $rankingsAfter) "Ranking-service chua cap nhat BXH cho competitionId=$compId"
Assert-True ($null -ne $notifFound) "Notification-service chua nhan su kien MATCH_FINISHED tu RabbitMQ cho matchId=$matchId"

$matchEvtLogs = Invoke-Compose @("logs", "--since", $sport02StartUtc, "match-service")
$notifEvtLogs = Invoke-Compose @("logs", "--since", $sport02StartUtc, "notification-service")
Set-Content -Path (Join-Path $ResultsDir "sport02-rabbitmq-logs.txt") -Value ("=== MATCH-SERVICE ===`n$matchEvtLogs`n`n=== NOTIFICATION-SERVICE ===`n$notifEvtLogs") -Encoding UTF8

$sortedRankingItems = @($rankingsAfter.items | Sort-Object rankPosition)
[ordered]@{
    competitionId = $compId
    matchId = $matchId
    matchFinalStatus = $matchAfter.status
    resultId = $resultId
    resultStatus = $confirmResultObj.status
    rankingItemsCount = $sortedRankingItems.Count
    topRankedTeam = $sortedRankingItems[0].participantName
    topRankedPoints = $sortedRankingItems[0].points
    notificationId = $notifFound.id
    notificationTitle = $notifFound.title
} | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $ResultsDir "sport02-e2e-flow.json") -Encoding UTF8
Write-Host "  [SPORT02] PASS (Flow + PostgreSQL + RabbitMQ MATCH_FINISHED -> Notif ID=$($notifFound.id))" -ForegroundColor Green
$summary["SPORT02"] = "PASS"

# ------------------------------------------------------------------------------
# SPORT03: PostgreSQL per-service user isolation & table ownership verification
# ------------------------------------------------------------------------------
Write-Host "`n[SPORT03] Kiem tra co lap tai khoan DB (<service>_user) va quyen so huu bang..." -ForegroundColor Yellow
$defaultDbPass = Get-Cfg "DB_PASSWORD" ""
$dbServices = @(
    @{ db = "auth_db"; user = "auth_user"; pass = (Get-Cfg "AUTH_DB_PASSWORD" $defaultDbPass) },
    @{ db = "tournament_db"; user = "tournament_user"; pass = (Get-Cfg "TOURNAMENT_DB_PASSWORD" $defaultDbPass) },
    @{ db = "team_db"; user = "team_user"; pass = (Get-Cfg "TEAM_DB_PASSWORD" $defaultDbPass) },
    @{ db = "athlete_db"; user = "athlete_user"; pass = (Get-Cfg "ATHLETE_DB_PASSWORD" $defaultDbPass) },
    @{ db = "match_db"; user = "match_user"; pass = (Get-Cfg "MATCH_DB_PASSWORD" $defaultDbPass) },
    @{ db = "result_db"; user = "result_user"; pass = (Get-Cfg "RESULT_DB_PASSWORD" $defaultDbPass) },
    @{ db = "ranking_db"; user = "ranking_user"; pass = (Get-Cfg "RANKING_DB_PASSWORD" $defaultDbPass) },
    @{ db = "notification_db"; user = "notification_user"; pass = (Get-Cfg "NOTIFICATION_DB_PASSWORD" $defaultDbPass) }
)

$teamDbPass = ($dbServices | Where-Object { $_.user -eq "team_user" } | Select-Object -First 1).pass
$prevEap = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$crossDbRaw = & docker compose -p $ComposeProjectName --env-file $EnvFile exec -T -e "PGPASSWORD=$teamDbPass" postgres-db psql -U team_user -d athlete_db -c "select 1" 2>&1
$crossDbExit = $LASTEXITCODE
$ErrorActionPreference = $prevEap
$crossDbText = ($crossDbRaw | ForEach-Object { $_.ToString() }) -join "`n"
Assert-True ($crossDbExit -ne 0 -and $crossDbText -match "permission denied") "team_user khong duoc phep ket noi athlete_db! exit=$crossDbExit, output=$crossDbText"

$ownDbChecks = @()
foreach ($entry in $dbServices) {
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $whoOut = & docker compose -p $ComposeProjectName --env-file $EnvFile exec -T -e "PGPASSWORD=$($entry.pass)" postgres-db psql -U $($entry.user) -d $($entry.db) -tAc "SELECT current_user;" 2>&1
    $whoExit = $LASTEXITCODE
    $ownerOut = & docker compose -p $ComposeProjectName --env-file $EnvFile exec -T -e "PGPASSWORD=$($entry.pass)" postgres-db psql -U $($entry.user) -d $($entry.db) -tAc "SELECT COALESCE(string_agg(tablename || ':' || tableowner, ',' ORDER BY tablename), '') FROM pg_tables WHERE schemaname = 'public';" 2>&1
    $ownerExit = $LASTEXITCODE
    $ErrorActionPreference = $prevEap

    $whoStr = (($whoOut | ForEach-Object { $_.ToString() }) -join "").Trim()
    $ownerStr = (($ownerOut | ForEach-Object { $_.ToString() }) -join "").Trim()
    Assert-True ($whoExit -eq 0 -and $whoStr -eq $entry.user) "Tai khoan $($entry.user) khong ket noi duoc $($entry.db) (exit=$whoExit, out=$whoStr)"
    Assert-True ($ownerExit -eq 0 -and -not [string]::IsNullOrWhiteSpace($ownerStr)) "Khong lay duoc danh sach bang thuoc schema public trong $($entry.db): $ownerStr"
    foreach ($pair in ($ownerStr -split ",")) {
        $parts = $pair -split ":"
        Assert-True ($parts.Count -eq 2 -and $parts[1] -eq $entry.user) "Bang '$($parts[0])' trong $($entry.db) khong thuoc so huu cua $($entry.user) (owner=$($parts[1]))"
    }
    $ownDbChecks += [ordered]@{
        database = $entry.db
        user = $entry.user
        currentUserVerified = $whoStr
        tablesAndOwners = ($ownerStr -split ",")
    }
}

[ordered]@{
    crossDbDeniedTest = [ordered]@{
        command = "psql -U team_user -d athlete_db -c ""select 1"""
        exitCode = $crossDbExit
        denied = $true
        message = $crossDbText.Trim()
    }
    serviceOwnershipChecks = $ownDbChecks
} | ConvertTo-Json -Depth 6 | Set-Content -Path (Join-Path $ResultsDir "sport03-db-user-isolation.json") -Encoding UTF8
Write-Host "  [SPORT03] PASS (team_user -> athlete_db bi tu choi; 8/8 DB thuoc quyen <service>_user)" -ForegroundColor Green
$summary["SPORT03"] = "PASS"

# ------------------------------------------------------------------------------
# SPORT04: End-to-end X-Request-Id correlation in Gateway + backend logs
# ------------------------------------------------------------------------------
Write-Host "`n[SPORT04] Kiem tra tuong quan ma X-Request-Id trong phan hoi & log Gateway + service..." -ForegroundColor Yellow
$traceId = "sport04-trace-$suffix"
$sport04StartUtc = (Get-Date).AddSeconds(-2).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")

$traceResp = Invoke-HttpJson "GET" "$baseGw/api/v1/tournaments/$tourId" $null @{ "X-Request-Id" = $traceId }
Assert-True ($traceResp.StatusCode -eq 200) "GET /api/v1/tournaments/$tourId qua Gateway that bai (status=$($traceResp.StatusCode))"
$rawReqId = $traceResp.Headers["X-Request-Id"]
[string[]]$respTraceHeaders = if ($null -eq $rawReqId) { @() } elseif ($rawReqId -is [string]) { @($rawReqId) } else { @($rawReqId) }
Assert-True ($respTraceHeaders.Count -eq 1 -and $respTraceHeaders[0] -eq $traceId) "Phan hoi Gateway phai co duy nhat 1 header X-Request-Id='$traceId', thuc te=$($respTraceHeaders -join ',')"

Start-Sleep -Seconds 1
$gwTraceLogs = Invoke-Compose @("logs", "--since", $sport04StartUtc, "api-gateway")
$t1TraceLogs = Invoke-Compose @("logs", "--since", $sport04StartUtc, "tournament-service")
$t2TraceLogs = Invoke-Compose @("logs", "--since", $sport04StartUtc, "tournament-service-2")

$gwMatchedLines = @($gwTraceLogs -split "`r?`n" | Where-Object { $_ -match [regex]::Escape($traceId) })
$tourMatchedLines = @((($t1TraceLogs + "`n" + $t2TraceLogs) -split "`r?`n") | Where-Object { $_ -match [regex]::Escape($traceId) })

Assert-True ($gwMatchedLines.Count -ge 1) "Khong tim thay X-Request-Id '$traceId' trong log api-gateway:`n$gwTraceLogs"
Assert-True ($tourMatchedLines.Count -ge 1) "Khong tim thay X-Request-Id '$traceId' trong log tournament-service/tournament-service-2!"

$traceLogBundle = @(
    "=== REQUEST ID: $traceId ===",
    "Response Header X-Request-Id count=$($respTraceHeaders.Count), value=$($respTraceHeaders[0])",
    "",
    "=== API-GATEWAY LOG LINES ===",
    ($gwMatchedLines -join "`n"),
    "",
    "=== TOURNAMENT-SERVICE LOG LINES ===",
    ($tourMatchedLines -join "`n")
) -join "`n"
Set-Content -Path (Join-Path $ResultsDir "sport04-request-id-logs.txt") -Value $traceLogBundle -Encoding UTF8

[ordered]@{
    requestId = $traceId
    endpoint = "/api/v1/tournaments/$tourId"
    statusCode = $traceResp.StatusCode
    responseHeaderValues = $respTraceHeaders
    gatewayLogMatches = $gwMatchedLines
    backendLogMatches = $tourMatchedLines
} | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $ResultsDir "sport04-request-id.json") -Encoding UTF8
Write-Host "  [SPORT04] PASS (X-Request-Id=$traceId xuat hien khop trong Gateway & tournament-service log)" -ForegroundColor Green
$summary["SPORT04"] = "PASS"

$summary | ConvertTo-Json -Depth 3 | Set-Content -Path (Join-Path $ResultsDir "summary.json") -Encoding UTF8
Write-Host "`n>>> TAT CA KIEM THU TH8 (DEP01..DEP06 + SPORT01..SPORT04) DEU PASS <<<" -ForegroundColor Green

if (-not $KeepRunning) {
    Write-Host "`nDang dung project kiem thu '$ComposeProjectName' (khong xoa volume)..." -ForegroundColor Cyan
    Invoke-Compose @("down") | Out-Null
}
