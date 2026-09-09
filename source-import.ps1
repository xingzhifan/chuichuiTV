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

function Test-DenyEntry([hashtable]$e, [string[]]$deny) {
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
    if ($env:SKIP_PROBE -eq "1") { return $true }
    $url = "$($api)?ac=list"
    try {
        $resp = Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec $timeout
        $txt = $resp.Content
        return ($resp.StatusCode -eq 200 -and ($txt.IndexOf('"class"') -ge 0 -or $txt.IndexOf('"list"') -ge 0))
    } catch { return $false }
}
