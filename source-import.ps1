param(
    [switch]$DryRun,
    [switch]$SkipProbe,
    [string]$UpstreamUrl = "https://mylazily.github.io/ziyuanzhan/data/online.json",
    [switch]$NoMain
)
$ErrorActionPreference = "Stop"
if ($SkipProbe) { $env:SKIP_PROBE = "1" }
$listPath = if ($env:SOURCE_LIST) { $env:SOURCE_LIST } else { "sources.json" }
$statePath = if ($env:IMPORT_STATE) { $env:IMPORT_STATE } else { ".source-import-state.json" }
$timeoutSec = if ($env:HTTP_TIMEOUT_SEC) { [int]$env:HTTP_TIMEOUT_SEC } else { 12 }
$probeTimeoutSec = $timeoutSec * 2
$COOLING_DAYS = 3

function Get-CanonicalApi([string]$api) {
    $s = $api.Trim()
    if ($s -match '^http://') { $s = "https://" + $s.Substring(7) }
    return $s.TrimEnd('/').ToLowerInvariant()
}

function Test-DenyEntry($e, [string[]]$deny) {
    $hay = (@($e.name, $e.api, $e.description) | Where-Object { $_ }) -join ' '
    foreach ($w in $deny) {
        if ($w.Trim().Length -eq 0) { continue }
        if ($hay.IndexOf($w.Trim(), [System.StringComparison]::OrdinalIgnoreCase) -ge 0) { return $true }
    }
    return $false
}

function Convert-ToSource([hashtable]$e) {
    return @{ name = $e.name; api = (Get-CanonicalApi $e.api); type = "maccms" }
}

function Get-IpList([string]$path) {
    return [System.IO.File]::ReadAllLines((Resolve-Path $path)) 
}

function Test-AcListAlive([string]$api, [int]$timeout) {
    return ($null -ne (Get-AcListJson $api $timeout))
}

function Get-AcListJson([string]$api, [int]$timeout) {
    if ($env:SKIP_PROBE -eq "1") { return [pscustomobject]@{} }
    $url = "$($api)?ac=list"
    try {
        $resp = Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec $timeout
        $txt = $resp.Content
        if ($resp.StatusCode -ne 200 -or ($txt.IndexOf('"class"') -lt 0 -and $txt.IndexOf('"list"') -lt 0)) { return $null }
        return ($txt | ConvertFrom-Json)
    } catch { return $null }
}

function Test-ContentDenyEntry($json, [string[]]$deny) {
    if ($null -eq $json -or $null -eq $json.class) { return $false }
    $hay = (@($json.class | ForEach-Object { $_.type_name } | Where-Object { $_ }) -join ' ')
    if ($hay.Length -eq 0) { return $false }
    foreach ($w in $deny) {
        if ($w.Trim().Length -eq 0) { continue }
        if ($hay.IndexOf($w.Trim(), [System.StringComparison]::OrdinalIgnoreCase) -ge 0) { return $true }
    }
    return $false
}

function Write-Sources([array]$list, [string]$path) {
    $arr = @($list | ForEach-Object { @{ name = $_.name; api = $_.api; type = "maccms" } })
    $json = $arr | ConvertTo-Json -Depth 4
    $full = [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $path))
    [System.IO.File]::WriteAllText($full, $json, (New-Object System.Text.UTF8Encoding($false)))
}

function Invoke-CooldownStep($state, [string]$api) {
    $removed = $state.removed
    $found = if ($removed -is [hashtable]) { $removed.ContainsKey($api) } else { $removed.PSObject.Properties.Name -contains $api }
    if ($null -ne $removed -and $found) {
        $rec = $removed.$api
        $newStreak = [int]$rec.onlineStreak + 1
        $reAdd = $newStreak -ge $COOLING_DAYS
        return @{ reAdd = $reAdd; onlineStreak = $newStreak; removedKey = $api }
    }
    return @{ reAdd = $true; onlineStreak = $null; removedKey = $null }
}

function Test-BlockedApi($state, [string]$api) {
    $blocked = $state.blocked
    if ($null -eq $blocked) { return $false }
    if ($blocked -is [hashtable]) { return $blocked.ContainsKey($api) }
    return ($blocked.PSObject.Properties.Name -contains $api)
}

function Add-BlockedApi($state, [string]$api, [string]$reason) {
    if ($null -eq $state.blocked -or $state.blocked -is [string]) {
        if ($state -is [hashtable]) {
            $state["blocked"] = @{}
        } elseif ($state.PSObject.Properties.Name -notcontains "blocked") {
            $state | Add-Member -Name "blocked" -Value ([hashtable]@{}) -MemberType NoteProperty -Force
        }
    }
    $rec = @{ blockedAt = (Get-Date -Format "yyyy-MM-dd"); reason = $reason }
    if ($state.blocked -is [hashtable]) {
        if (-not $state.blocked.ContainsKey($api)) { $state.blocked[$api] = $rec }
    } else {
        if ($state.blocked.PSObject.Properties.Name -notcontains $api) {
            $state.blocked | Add-Member -Name $api -Value $rec -MemberType NoteProperty -Force
        }
    }
}

function Invoke-ImportMerge($upstream, $existing, [string[]]$deny, [string[]]$contentDeny, $state) {
    $existingMap = @{}
    foreach ($s in @($existing)) {
        $canon = Get-CanonicalApi $s.api
        $existingMap[$canon] = $s
    }
    $out = New-Object System.Collections.ArrayList
    foreach ($s in @($existing)) {
        $meta = Find-UpstreamMeta $upstream (Get-CanonicalApi $s.api)
        [void]$out.Add(@{
            name = $s.name; api = $s.api; type = "maccms"
            rating    = if ($meta) { [double]$meta.rating } else { 0.0 }
            resp = if ($meta) { [int]$meta.responseTime } else { [int]::MaxValue }
        })
    }
    $seen = @{}
    foreach ($e in @($upstream.data)) {
        if (-not $e.health.is_alive) { Write-Host "SKIP-DEAD: $($e.name)"; continue }
        if (Test-DenyEntry $e $deny) { Write-Host "SKIP-DENY: $($e.name)"; continue }
        if (-not ($e.api -match '^https?://')) { Write-Host "SKIP-NOURL: $($e.name)"; continue }
        $canon = Get-CanonicalApi $e.api
        if ($existingMap.ContainsKey($canon) -or $seen.ContainsKey($canon)) { Write-Host "SKIP-DUP: $($e.name)"; continue }
        if (Test-BlockedApi $state $canon) { Write-Host "SKIP-BLOCKED: $($e.name)"; continue }
        $cd = Invoke-CooldownStep $state $canon
        if ($cd.reAdd -eq $false) {
            Write-Host "COOLING: $($e.name) (streak $($cd.onlineStreak)/$COOLING_DAYS)"
            $rec = $state.removed.$canon
            $rec.onlineStreak = $cd.onlineStreak
            continue
        }
        $aliveJson = Get-AcListJson $e.api $probeTimeoutSec
        if ($null -eq $aliveJson) { Write-Host "SKIP-UNRESPONSIVE: $($e.name)"; continue }
        if (Test-ContentDenyEntry $aliveJson $contentDeny) {
            Add-BlockedApi $state $canon "adult-content-deny"
            Write-Host "SKIP-ADULT: $($e.name)"
            continue
        }
        $seen[$canon] = $true
        Write-Host "NEW: $($e.name) ($canon)"
        [void]$out.Add(@{
            name = $e.name; api = (Get-CanonicalApi $e.api); type = "maccms"
            rating = [double]$e.rating; resp = [int]$e.responseTime
        })
    }
    $sorted = @($out | Sort-Object -Property @{Expression={$_.rating}; Descending=$true}, @{Expression={$_.resp}; Descending=$false})
    return $sorted
}

function Find-UpstreamMeta($upstream, [string]$canon) {
    foreach ($e in @($upstream.data)) {
        if ((Get-CanonicalApi $e.api) -eq $canon) { return $e }
    }
    return $null
}

if ($NoMain) { return }

# ---- main ----
$deny = Get-IpList (Join-Path $PSScriptRoot "source-deny.txt") | Where-Object { $_ -and -not $_.StartsWith("#") }
$contentDeny = Get-IpList (Join-Path $PSScriptRoot "source-content-deny.txt") | Where-Object { $_ -and -not $_.StartsWith("#") }
$state = if (Test-Path $statePath) { (Get-Content $statePath -Raw -Encoding UTF8 | ConvertFrom-Json) } else { @{ removed = @{} } }
$existing = if (Test-Path $listPath) { @(@(Get-Content $listPath -Raw -Encoding UTF8 | ConvertFrom-Json)) } else { @() }
$upstreamRaw = if ($UpstreamUrl -match '^file://') {
    Get-Content ($UpstreamUrl -replace '^file://', '') -Raw -Encoding UTF8 | ConvertFrom-Json
} else {
    (Invoke-WebRequest -UseBasicParsing -Uri $UpstreamUrl -TimeoutSec $probeTimeoutSec).Content | ConvertFrom-Json
}
$merged = Invoke-ImportMerge $upstreamRaw $existing $deny $contentDeny $state

if ($DryRun) {
    Write-Output "DRYRUN: would write $(@($merged).Count) sources to $listPath"
    exit 0
}

Write-Sources $merged $listPath
$stateFull = [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $statePath))
[System.IO.File]::WriteAllText($stateFull, ($state | ConvertTo-Json -Depth 6), (New-Object System.Text.UTF8Encoding($false)))
Write-Output "SUMMARY: imported=$(@($merged).Count)"
