# probe-sources.ps1 - probe aliveness of each source and rewrite sources.json
# Usage: pwsh probe-sources.ps1
# Overridable env: SOURCE_LIST (default ./sources.json), HTTP_TIMEOUT_SEC (default 12)

$ErrorActionPreference = "Stop"
$listPath = if ($env:SOURCE_LIST) { $env:SOURCE_LIST } else { "sources.json" }
$timeoutSec = if ($env:HTTP_TIMEOUT_SEC) { [int]$env:HTTP_TIMEOUT_SEC } else { 12 }
$probeTimeoutSec = $timeoutSec * 2

if (-not (Test-Path $listPath)) {
    Write-Output "NO_LIST:$listPath"
    exit 1
}

$raw = [System.IO.File]::ReadAllText((Resolve-Path $listPath), [System.Text.Encoding]::UTF8)
if ($raw.Length -eq 0) {
    Write-Output "EMPTY_LIST:$listPath"
    exit 1
}
$sources = $raw | ConvertFrom-Json
if ($sources -isnot [array]) {
    $sources = @($sources)
}
if ($sources.Count -eq 0) {
    Write-Output "EMPTY_LIST:$listPath"
    exit 1
}

$alive = New-Object System.Collections.ArrayList
$dead = New-Object System.Collections.ArrayList

foreach ($s in $sources) {
    $name = $s.name
    $url = "$($s.api)?ac=list"
    $ok = $false
    try {
        $resp = Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec $probeTimeoutSec
        $txt = $resp.Content
        $hasClass = $txt.IndexOf('"class"') -ge 0
        $hasList = $txt.IndexOf('"list"') -ge 0
        if ($resp.StatusCode -eq 200 -and ($hasClass -or $hasList)) {
            $ok = $true
        }
    } catch {
        $ok = $false
    }
    if ($ok) {
        [void]$alive.Add($s)
        Write-Output "ALIVE: $name ($url)"
    } else {
        [void]$dead.Add($s)
        Write-Output "DEAD : $name ($url)"
    }
}

if ($alive.Count -gt 0) {
    $json = $alive | ConvertTo-Json -Depth 4
    [System.IO.File]::WriteAllText((Resolve-Path $listPath), $json, (New-Object System.Text.UTF8Encoding $false))
}
Write-Output "SUMMARY: total=$($sources.Count) alive=$($alive.Count) dead=$($dead.Count)"
