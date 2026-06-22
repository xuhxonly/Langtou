$BaseUrl = "http://localhost:8089"
$TestUserId = 1
$Results = New-Object System.Collections.ArrayList
$script:Passed = 0
$script:Failed = 0
$AuthHeaders = @{ "X-User-Id" = "$TestUserId" }

function Invoke-Http {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [hashtable]$Headers = @{}
    )
    $uri = "$BaseUrl$Path"
    try {
        if ($null -ne $Body -and ($Body -is [hashtable] -or $Body -is [PSCustomObject])) {
            $json = $Body | ConvertTo-Json -Depth 10
            $h = @{ "Content-Type" = "application/json" }
            foreach ($k in $Headers.Keys) { $h[$k] = $Headers[$k] }
            $resp = Invoke-WebRequest -Uri $uri -Method $Method -Headers $h -Body $json -TimeoutSec 30 -ErrorAction Stop
        } else {
            $resp = Invoke-WebRequest -Uri $uri -Method $Method -Headers $Headers -TimeoutSec 30 -ErrorAction Stop
        }
        return [PSCustomObject]@{
            StatusCode = [int]$resp.StatusCode
            Content    = $resp.Content
            Headers    = $resp.Headers
        }
    } catch {
        $statusCode = $null
        $content = $_.Exception.Message
        if ($_.Exception.Response) {
            try {
                $statusCode = [int]$_.Exception.Response.StatusCode
                $stream = $_.Exception.Response.GetResponseStream()
                if ($stream) {
                    $reader = New-Object System.IO.StreamReader($stream)
                    $content = $reader.ReadToEnd()
                    $reader.Close()
                }
            } catch {}
        }
        return [PSCustomObject]@{
            StatusCode = $statusCode
            Content    = $content
            Headers    = @{}
            Error      = $_.Exception.Message
        }
    }
}

function Assert-Case {
    param(
        [string]$Id,
        [string]$Category,
        [string]$Title,
        [scriptblock]$Check,
        [string]$Severity = "normal"
    )
    Write-Host "[$Id] $Category - $Title" -ForegroundColor Cyan
    try {
        $result = & $Check
        if ($result.Pass) {
            Write-Host "  PASS: $($result.Message)" -ForegroundColor Green
            $script:Passed++
            [void]$script:Results.Add([PSCustomObject]@{
                Id = $Id; Category = $Category; Title = $Title
                Status = "PASS"; Severity = $Severity; Detail = $result.Message
            })
        } else {
            Write-Host "  FAIL: $($result.Message)" -ForegroundColor Red
            $script:Failed++
            [void]$script:Results.Add([PSCustomObject]@{
                Id = $Id; Category = $Category; Title = $Title
                Status = "FAIL"; Severity = $Severity; Detail = $result.Message
            })
        }
        return $result
    } catch {
        Write-Host "  ERROR: $($_.Exception.Message)" -ForegroundColor Yellow
        $script:Failed++
        [void]$script:Results.Add([PSCustomObject]@{
            Id = $Id; Category = $Category; Title = $Title
            Status = "ERROR"; Severity = $Severity; Detail = $_.Exception.Message
        })
        return $null
    }
}

Write-Host "===== 0. Health =====" -ForegroundColor Magenta
$r = Invoke-Http -Method GET -Path "/actuator/health"
Write-Host "GET /actuator/health => Status=$($r.StatusCode)"
if ($r.Content) { Write-Host "  Body: $($r.Content.Substring(0, [Math]::Min(300, $r.Content.Length)))" }

Write-Host "`n===== 1. Creator =====" -ForegroundColor Magenta
$script:quizSetId = $null

Assert-Case "C1" "Creator" "POST /api/v1/quiz/generate returns QuizSet" {
    $r = Invoke-Http -Method POST -Path "/api/v1/quiz/generate" -Body @{ noteId = 1; questionCount = 5 } -Headers $AuthHeaders
    $ok = ($r.StatusCode -eq 200)
    $msg = "Status=$($r.StatusCode)"
    if ($ok) {
        try {
            $obj = $r.Content | ConvertFrom-Json
            $data = $obj.data
            if ($data.id) { $script:quizSetId = $data.id }
            $msg += " quizSetId=$script:quizSetId"
            $jsonText = ($data | ConvertTo-Json -Depth 5)
            if ($jsonText -match "questions") { $msg += " has questions" }
            if ($jsonText -match "questionCount") { $msg += " questionCount=$($data.questionCount)" }
        } catch {
            $msg += " non-json"
        }
    } else {
        $msg += " Body=$($r.Content.Substring(0, [Math]::Min(300, $r.Content.Length)))"
    }
    [PSCustomObject]@{ Pass = $ok; Message = $msg }
}

Assert-Case "C2" "Creator" "Generate with different questionCount" {
    $r = Invoke-Http -Method POST -Path "/api/v1/quiz/generate" -Body @{ noteId = 1; questionCount = 3 } -Headers $AuthHeaders
    $ok = ($r.StatusCode -eq 200)
    [PSCustomObject]@{ Pass = $ok; Message = "Status=$($r.StatusCode)" }
}

Assert-Case "C3" "Creator" "AI degrade: invalid noteId returns error" {
    $r = Invoke-Http -Method POST -Path "/api/v1/quiz/generate" -Body @{ noteId = 99999; questionCount = 5 } -Headers $AuthHeaders
    $ok = ($r.StatusCode -ge 400)
    [PSCustomObject]@{ Pass = $ok; Message = "Status=$($r.StatusCode) Body=$($r.Content.Substring(0,[Math]::Min(200,$r.Content.Length)))" }
}

Assert-Case "C4" "Creator" "My quiz sets pagination" {
    $r = Invoke-Http -Method GET -Path "/api/v1/quiz/sets/my" -Headers $AuthHeaders
    $ok = ($r.StatusCode -eq 200)
    [PSCustomObject]@{ Pass = $ok; Message = "Status=$($r.StatusCode)" }
}

if (-not $script:quizSetId) {
    $rFb = Invoke-Http -Method GET -Path "/api/v1/quiz/sets/my" -Headers $AuthHeaders
    if ($rFb.StatusCode -eq 200) {
        try {
            $obj = $rFb.Content | ConvertFrom-Json
            $list = $obj.data.records
            if ($list -and $list.Count -gt 0) { $script:quizSetId = $list[0].id }
        } catch {}
    }
    if (-not $script:quizSetId) { $script:quizSetId = 1 }
}
Write-Host ">>> quizSetId = $script:quizSetId"

Write-Host "`n===== 2. Player =====" -ForegroundColor Magenta
$script:attemptId = $null

Assert-Case "P1" "Player" "GET /api/v1/quiz/sets/{id} returns QuizSet" {
    $r = Invoke-Http -Method GET -Path "/api/v1/quiz/sets/$script:quizSetId" -Headers $AuthHeaders
    $ok = ($r.StatusCode -eq 200)
    [PSCustomObject]@{ Pass = $ok; Message = "Status=$($r.StatusCode)" }
}

Assert-Case "P2" "Player" "POST /api/v1/quiz/attempts starts attempt" {
    $r = Invoke-Http -Method POST -Path "/api/v1/quiz/attempts" -Body @{ quizSetId = $script:quizSetId } -Headers $AuthHeaders
    $ok = ($r.StatusCode -eq 200)
    if ($ok) {
        try {
            $obj = $r.Content | ConvertFrom-Json
            if ($obj.data.id) { $script:attemptId = $obj.data.id }
        } catch {}
    }
    [PSCustomObject]@{ Pass = $ok; Message = "Status=$($r.StatusCode) attemptId=$script:attemptId" }
}

if (-not $script:attemptId) { $script:attemptId = 1 }

Assert-Case "P3" "Player" "POST submit returns score and rank" {
    $body = @{
        attemptId       = $script:attemptId
        answers         = @(@{ sequenceNo = 1; selected = "A" }, @{ sequenceNo = 2; selected = "B" })
        durationSeconds = 30
    }
    $r = Invoke-Http -Method POST -Path "/api/v1/quiz/attempts/$script:attemptId/submit" -Body $body -Headers $AuthHeaders
    $msg = "Status=$($r.StatusCode)"
    $ok = ($r.StatusCode -eq 200)
    if ($ok) {
        try {
            $obj = $r.Content | ConvertFrom-Json
            $jsonText = ($obj.data | ConvertTo-Json -Depth 5)
            if ($jsonText -match "score") { $msg += " has score" }
            if ($jsonText -match "rank") { $msg += " has rank" }
            if ($jsonText -match "passed") { $msg += " has passed" }
        } catch {}
    } else {
        $msg += " Body=$($r.Content.Substring(0,[Math]::Min(300,$r.Content.Length)))"
    }
    [PSCustomObject]@{ Pass = $ok; Message = $msg }
}

Assert-Case "P4" "Player" "GET /api/v1/quiz/attempts/my returns history" {
    $r = Invoke-Http -Method GET -Path "/api/v1/quiz/attempts/my" -Headers $AuthHeaders
    $ok = ($r.StatusCode -eq 200)
    [PSCustomObject]@{ Pass = $ok; Message = "Status=$($r.StatusCode)" }
}

Write-Host "`n===== 3. Social =====" -ForegroundColor Magenta

Assert-Case "S1" "Social" "GET /api/v1/quiz/leaderboard/global" {
    $r = Invoke-Http -Method GET -Path "/api/v1/quiz/leaderboard/global"
    $ok = ($r.StatusCode -eq 200)
    [PSCustomObject]@{ Pass = $ok; Message = "Status=$($r.StatusCode)" }
}

Assert-Case "S2" "Social" "GET /api/v1/quiz/leaderboard/quiz/{setId}" {
    $r = Invoke-Http -Method GET -Path "/api/v1/quiz/leaderboard/quiz/$script:quizSetId"
    $ok = ($r.StatusCode -eq 200)
    [PSCustomObject]@{ Pass = $ok; Message = "Status=$($r.StatusCode)" }
}

Assert-Case "S3" "Social" "GET /api/v1/quiz/leaderboard/friends" {
    $r = Invoke-Http -Method GET -Path "/api/v1/quiz/leaderboard/friends" -Headers $AuthHeaders
    $ok = ($r.StatusCode -eq 200)
    [PSCustomObject]@{ Pass = $ok; Message = "Status=$($r.StatusCode)" }
}

Write-Host "`n===== 4. Edge Cases =====" -ForegroundColor Magenta

Assert-Case "E1" "Edge" "Not-exist setId returns 404" "high" {
    $r = Invoke-Http -Method GET -Path "/api/v1/quiz/sets/999999999" -Headers $AuthHeaders
    $ok = ($r.StatusCode -eq 404)
    [PSCustomObject]@{ Pass = $ok; Message = "Status=$($r.StatusCode)" }
}

Assert-Case "E2" "Edge" "Duplicate submit returns 4xx" "high" {
    $body = @{ attemptId = $script:attemptId; answers = @(@{ sequenceNo = 1; selected = "A" }); durationSeconds = 10 }
    $r = Invoke-Http -Method POST -Path "/api/v1/quiz/attempts/$script:attemptId/submit" -Body $body -Headers $AuthHeaders
    $ok = ($r.StatusCode -ge 400 -and $r.StatusCode -lt 500)
    [PSCustomObject]@{ Pass = $ok; Message = "Status=$($r.StatusCode) Body=$($r.Content.Substring(0,[Math]::Min(200,$r.Content.Length)))" }
}

Assert-Case "E3" "Edge" "Exceed duration returns 4xx" "high" {
    $na = Invoke-Http -Method POST -Path "/api/v1/quiz/attempts" -Body @{ quizSetId = $script:quizSetId } -Headers $AuthHeaders
    $newAid = $script:attemptId
    if ($na.StatusCode -eq 200) {
        try {
            $o = $na.Content | ConvertFrom-Json
            if ($o.data.id) { $newAid = $o.data.id }
        } catch {}
    }
    $body = @{ attemptId = $newAid; answers = @(@{ sequenceNo = 1; selected = "A" }); durationSeconds = 999999 }
    $r = Invoke-Http -Method POST -Path "/api/v1/quiz/attempts/$newAid/submit" -Body $body -Headers $AuthHeaders
    $ok = ($r.StatusCode -ge 400 -and $r.StatusCode -lt 500)
    [PSCustomObject]@{ Pass = $ok; Message = "Status=$($r.StatusCode)" }
}

Assert-Case "E4" "Edge" "Empty answers returns 4xx" "high" {
    $na = Invoke-Http -Method POST -Path "/api/v1/quiz/attempts" -Body @{ quizSetId = $script:quizSetId } -Headers $AuthHeaders
    $newAid = $script:attemptId
    if ($na.StatusCode -eq 200) {
        try {
            $o = $na.Content | ConvertFrom-Json
            if ($o.data.id) { $newAid = $o.data.id }
        } catch {}
    }
    $body = @{ attemptId = $newAid; answers = @(); durationSeconds = 5 }
    $r = Invoke-Http -Method POST -Path "/api/v1/quiz/attempts/$newAid/submit" -Body $body -Headers $AuthHeaders
    $ok = ($r.StatusCode -ge 400 -and $r.StatusCode -lt 500)
    [PSCustomObject]@{ Pass = $ok; Message = "Status=$($r.StatusCode)" }
}

Assert-Case "E5" "Edge" "Invalid attemptId returns 4xx" "high" {
    $body = @{ attemptId = 99999999; answers = @(@{ sequenceNo = 1; selected = "A" }); durationSeconds = 5 }
    $r = Invoke-Http -Method POST -Path "/api/v1/quiz/attempts/99999999/submit" -Body $body -Headers $AuthHeaders
    $ok = ($r.StatusCode -ge 400 -and $r.StatusCode -lt 500)
    [PSCustomObject]@{ Pass = $ok; Message = "Status=$($r.StatusCode)" }
}

Write-Host "`n===== 5. Performance =====" -ForegroundColor Magenta

function Measure-Perf {
    param([string]$Path, [int]$Runs = 20, [hashtable]$Headers = @{})
    $times = @()
    $statusOk = $true
    for ($i = 0; $i -lt $Runs; $i++) {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $r = Invoke-Http -Method GET -Path $Path -Headers $Headers
        $sw.Stop()
        $times += $sw.ElapsedMilliseconds
        if ($r.StatusCode -ne 200) { $statusOk = $false }
    }
    $sorted = $times | Sort-Object
    $p50 = $sorted[[int][Math]::Floor($Runs * 0.5)]
    $p95 = $sorted[[int][Math]::Floor($Runs * 0.95)]
    $p99 = $sorted[[int][Math]::Floor($Runs * 0.99)]
    $avg = [int](($times | Measure-Object -Average).Average)
    [PSCustomObject]@{ P50 = $p50; P95 = $p95; P99 = $p99; Avg = $avg; StatusOk = $statusOk; Samples = $Runs; Times = $times }
}

Assert-Case "PF1" "Perf" "QuizSet load P95 less than 500ms" {
    $m = Measure-Perf -Path "/api/v1/quiz/sets/$script:quizSetId" -Runs 20 -Headers $AuthHeaders
    $ok = $m.P95 -lt 500
    [PSCustomObject]@{ Pass = $ok; Message = "P50=$($m.P50)ms P95=$($m.P95)ms P99=$($m.P99)ms Avg=$($m.Avg)ms" }
}

Assert-Case "PF2" "Perf" "Submit P95 less than 1s" {
    $times = @()
    $okFlag = $true
    for ($i = 0; $i -lt 8; $i++) {
        $na = Invoke-Http -Method POST -Path "/api/v1/quiz/attempts" -Body @{ quizSetId = $script:quizSetId } -Headers $AuthHeaders
        $aid = $script:attemptId
        if ($na.StatusCode -eq 200) {
            try {
                $o = $na.Content | ConvertFrom-Json
                if ($o.data.id) { $aid = $o.data.id }
            } catch {}
        }
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $r = Invoke-Http -Method POST -Path "/api/v1/quiz/attempts/$aid/submit" -Body @{ attemptId = $aid; answers = @(@{ sequenceNo = 1; selected = "A" }); durationSeconds = 10 } -Headers $AuthHeaders
        $sw.Stop()
        $times += $sw.ElapsedMilliseconds
        if ($r.StatusCode -ne 200) { $okFlag = $false }
    }
    $sorted = $times | Sort-Object
    $p95 = $sorted[[int][Math]::Floor($times.Count * 0.95)]
    $ok = $p95 -lt 1000
    [PSCustomObject]@{ Pass = $ok -and $okFlag; Message = "P95=${p95}ms okFlag=$okFlag" }
}

Assert-Case "PF3" "Perf" "Leaderboard P95 less than 200ms" {
    $m = Measure-Perf -Path "/api/v1/quiz/leaderboard/global" -Runs 20
    $ok = $m.P95 -lt 200
    [PSCustomObject]@{ Pass = $ok; Message = "P50=$($m.P50)ms P95=$($m.P95)ms P99=$($m.P99)ms Avg=$($m.Avg)ms" }
}

Write-Host "`n===== 6. Security =====" -ForegroundColor Magenta

Assert-Case "SEC1" "Security" "Write op without X-User-Id returns 4xx" "high" {
    $r = Invoke-Http -Method POST -Path "/api/v1/quiz/attempts" -Body @{ quizSetId = $script:quizSetId }
    $ok = ($r.StatusCode -eq 400) -or ($r.StatusCode -eq 401) -or ($r.StatusCode -eq 403)
    [PSCustomObject]@{ Pass = $ok; Message = "Status=$($r.StatusCode)" }
}

Assert-Case "SEC2" "Security" "User isolation - access other's attempt" "high" {
    $otherHeaders = @{ "X-User-Id" = "99999" }
    $r = Invoke-Http -Method GET -Path "/api/v1/quiz/attempts/$script:attemptId" -Headers $otherHeaders
    $ok = ($r.StatusCode -ge 400 -and $r.StatusCode -lt 500)
    [PSCustomObject]@{ Pass = $ok; Message = "Status=$($r.StatusCode) Body=$($r.Content.Substring(0,[Math]::Min(200,$r.Content.Length)))" }
}

Assert-Case "SEC3" "Security" "My endpoint without X-User-Id returns 4xx" "high" {
    $r = Invoke-Http -Method GET -Path "/api/v1/quiz/attempts/my"
    $ok = ($r.StatusCode -ge 400 -and $r.StatusCode -lt 500)
    [PSCustomObject]@{ Pass = $ok; Message = "Status=$($r.StatusCode)" }
}

Assert-Case "SEC4" "Security" "Rate limit stability" {
    $r429 = $false
    $pass = $true
    $start = Get-Date
    for ($i = 0; $i -lt 80; $i++) {
        $r = Invoke-Http -Method GET -Path "/api/v1/quiz/sets/$script:quizSetId" -Headers $AuthHeaders
        if ($r.StatusCode -eq 429) { $r429 = $true; break }
        if (($r.StatusCode -ne 200) -and ($r.StatusCode -ne 429)) { $pass = $false }
    }
    $dur = [math]::Round(((Get-Date) - $start).TotalSeconds, 2)
    [PSCustomObject]@{ Pass = $pass; Message = "80 requests in ${dur}s 429=$r429" }
}

Write-Host "`n===== Summary =====" -ForegroundColor Magenta
$Total = $script:Passed + $script:Failed
if ($Total -eq 0) { $Total = 1 }
$Rate = [math]::Round(($script:Passed / $Total) * 100, 2)
Write-Host "Total: $Total  Passed: $script:Passed  Failed: $script:Failed  Rate: $Rate%"

Write-Host "`n--- Details ---"
$script:Results | Format-Table Id, Category, Title, Status, Severity, Detail -AutoSize -Wrap

Write-Host "`n--- Issues ---"
$issues = $script:Results | Where-Object { $_.Status -ne "PASS" }
if ($issues.Count -eq 0) {
    Write-Host "(none)"
} else {
    $issues | ForEach-Object {
        Write-Host "[$($_.Severity.ToUpper())] $($_.Id) $($_.Title): $($_.Detail)"
    }
}

Write-Host "`n--- Verdict ---"
if ($Rate -ge 90) {
    $verdict = "PASS (rate=$Rate% >= 90%)"
    Write-Host "Verdict: $verdict" -ForegroundColor Green
} elseif ($Rate -ge 75) {
    $verdict = "CONDITIONAL PASS (rate=$Rate%)"
    Write-Host "Verdict: $verdict" -ForegroundColor Yellow
} else {
    $verdict = "FAIL (rate=$Rate% < 75%)"
    Write-Host "Verdict: $verdict" -ForegroundColor Red
}

$outPath = Join-Path (Get-Location) "quiz_test_report.json"
$exportObj = @{
    summary   = @{ total = $Total; passed = $script:Passed; failed = $script:Failed; rate = $Rate; verdict = $verdict }
    results   = $script:Results
    timestamp = (Get-Date).ToString("s")
}
$exportObj | ConvertTo-Json -Depth 6 | Out-File -FilePath $outPath -Encoding UTF8
Write-Host "Report: $outPath"
