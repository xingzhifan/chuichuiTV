$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "..\source-import.ps1") -NoMain

$script:fail = 0
function Assert([bool]$cond, [string]$msg) {
    if (-not $cond) { $script:fail += 1; Write-Host "FAIL: $msg" } else { Write-Host "ok: $msg" }
}

# Get-CanonicalApi
$c1 = Get-CanonicalApi "https://jipinvip1.com/api.php/provide/vod/"
$c2 = Get-CanonicalApi "https://jipinvip1.com/api.php/provide/vod"
Assert ($c1 -eq $c2) "canonical api trims trailing slash"
Assert ((Get-CanonicalApi "HTTP://360ZY.com/api.php/PROVIDE/vod/") -eq "https://360zy.com/api.php/provide/vod") "scheme+domain normalized, path lowercased"

# Test-DenyEntry
Assert (Test-DenyEntry @{name="老色X资源站"; api="https://apilsbzy1.com"; description=""} @("老色","伦理")) "deny hits name"
Assert (-not (Test-DenyEntry @{name="奥迪资源站"; api="https://aodi.com"; description="无广告"} @("老色","伦理"))) "clean name passes"
Assert (Test-DenyEntry @{name="影视站"; api="https://x.com/91/vod"; description=""} @("老色","伦理","91")) "deny hits api substring"

# Test-AcListAlive (offline: uses SkipProbe via env, see task 3) — placeholder assertion removed: HTTP only in CI

# Convert-ToSource
$src = Convert-ToSource @{name="新极品资源站"; api="https://jipinvip1.com/api.php/provide/vod/"; rating=4.8; responseTime=693}
Assert ($src.name -eq "新极品资源站" -and $src.api -eq "https://jipinvip1.com/api.php/provide/vod" -and $src.type -eq "maccms") "to-source maps fields and strips trailing slash"

# Get-SortKeys / ordering handled in task 3 fixture merge test

# ---- task 3: Invoke-ImportMerge (pure, offline) ----
$env:SKIP_PROBE = "1"
$up = Get-Content (Join-Path $PSScriptRoot "fixtures\upstream-good.json") -Raw | ConvertFrom-Json
$initial = Get-Content (Join-Path $PSScriptRoot "fixtures\sources-initial.json") -Raw | ConvertFrom-Json
$deny = Get-IpList (Join-Path $PSScriptRoot "..\source-deny.txt") | Where-Object { $_ -and -not $_.StartsWith("#") }
$contentDeny = Get-IpList (Join-Path $PSScriptRoot "..\source-content-deny.txt") | Where-Object { $_ -and -not $_.StartsWith("#") }
$state = @{ removed = @{}; blocked = @{} }
$merged = Invoke-ImportMerge $up $initial $deny $contentDeny $state
$apis = @($merged | ForEach-Object { $_.api })
Assert ($apis -contains "https://jipinvip1.com/api.php/provide/vod") "new good source merged"
Assert (-not ($apis -contains "https://apilsbzy1.com/api.php/provide/vod")) "deny-word source not merged"
Assert (-not ($apis -contains "https://dead.example.com/api.php/provide/vod")) "dead source not merged"
Assert (@($apis | Where-Object { $_ -eq "https://360zy.com/api.php/provide/vod" }).Count -eq 1) "existing source kept, not duplicated"
Assert ($merged.Count -eq 5) "expected 5 (3 existing + 2 new)"
$sorted = @($merged | ForEach-Object { $_.api })
Assert ($sorted[0] -eq "https://jipinvip1.com/api.php/provide/vod") "sort rating desc: 4.8 first"
Assert ($sorted[1] -eq "https://api.wujinapi.me/api.php/provide/vod" -and $sorted[2] -eq "https://360zy.com/api.php/provide/vod") "tie on rating 4.5 sorted by resp asc (354 < 952)"
Assert ($sorted[4] -eq "http://cj.lziapi.com/api.php/provide/vod/") "existing source without upstream data last (original api preserved)"

# ---- task 7: content-layer deny (Test-ContentDenyEntry) ----
$adultJson = [pscustomobject]@{ class = @(@{ type_name = "日韩无码"; type_id = 1 }, @{ type_name = "偷拍区"; type_id = 2 }) }
$cleanJson = [pscustomobject]@{ class = @(@{ type_name = "电影"; type_id = 1 }, @{ type_name = "伦理"; type_id = 2 }, @{ type_name = "动作片"; type_id = 3 }) }
$listOnlyJson = [pscustomobject]@{ list = @() }
Assert (Test-ContentDenyEntry $adultJson $contentDeny) "content deny hits strong adult term (无码)"
Assert (-not (Test-ContentDenyEntry $cleanJson $contentDeny)) "weak term 伦理 alone does not trigger content deny"
Assert (-not (Test-ContentDenyEntry $listOnlyJson $contentDeny)) "no class array → no content deny"
Assert (-not (Test-ContentDenyEntry $null $contentDeny)) "null json → no content deny"

# ---- task 7: blocked set (permanent, no cooldown) ----
$overlap = @{ "https://api.wujinapi.me/api.php/provide/vod" = @{ blockedAt = "2026-09-10"; reason = "adult-content-deny" } }
$stateB = @{ removed = @{}; blocked = $overlap }
$mergedB = Invoke-ImportMerge $up $initial $deny $contentDeny $stateB
$apisB = @($mergedB | ForEach-Object { $_.api })
Assert (-not ($apisB -contains "https://api.wujinapi.me/api.php/provide/vod")) "blocked source not merged (permanent)"
Assert (Test-BlockedApi $stateB "https://api.wujinapi.me/api.php/provide/vod") "Test-BlockedApi true for blocked key"
Assert (-not (Test-BlockedApi $stateB "https://127.0.0.1/nope")) "Test-BlockedApi false for unknown key"
$stB2 = @{ removed = @{}; blocked = @{} }
Add-BlockedApi $stB2 "https://api.wujinapi.me/api.php/provide/vod" "adult-content-deny"
Assert (Test-BlockedApi $stB2 "https://api.wujinapi.me/api.php/provide/vod") "Add-BlockedApi records canonical key"
Assert ($stB2.blocked["https://api.wujinapi.me/api.php/provide/vod"].reason -eq "adult-content-deny") "Add-BlockedApi stores canonical reason"

# ---- task 3b: cooldown machine ----
$st2 = @{ removed = @{ "https://dead.ex.com/api.php/provide/vod" = @{ removedAt = "2026-09-01"; onlineStreak = 2 } } }
$res = Invoke-CooldownStep $st2 "https://dead.ex.com/api.php/provide/vod"
Assert ($res.onlineStreak -eq 3 -and $res.reAdd -eq $true) "cooldown reaches 3 → re-add"
$st3 = @{ removed = @{ "https://dead.ex.com/api.php/provide/vod" = @{ removedAt = "2026-09-01"; onlineStreak = 1 } } }
$res2 = Invoke-CooldownStep $st3 "https://dead.ex.com/api.php/provide/vod"
Assert ($res2.onlineStreak -eq 2 -and $res2.reAdd -eq $false) "cooldown below threshold → cooling off"
$st4 = @{ removed = @{} }
$res3 = Invoke-CooldownStep $st4 "https://fresh.example.com/pkg/vod"
Assert ($res3.reAdd -eq $true -and $null -eq $res3.onlineStreak) "absent from removed → direct merge"

if ($script:fail -gt 0) { exit 1 }
Write-Host "ALL PASS"
exit 0