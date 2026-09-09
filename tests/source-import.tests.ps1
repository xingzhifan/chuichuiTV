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

if ($script:fail -gt 0) { exit 1 }
Write-Host "ALL PASS"
exit 0