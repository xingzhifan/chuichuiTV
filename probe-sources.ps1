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
$adult = New-Object System.Collections.ArrayList

$contentDenyPath = Join-Path $PSScriptRoot "source-content-deny.txt"
$contentDeny = if (Test-Path $contentDenyPath) { (Get-Content $contentDenyPath -Encoding UTF8 | Where-Object { $_ -and -not $_.StartsWith("#") }) } else { @() }

foreach ($s in $sources) {
    $name = $s.name
    $url = "$($s.api)?ac=list"
    $ok = $false
    $adultHit = $false
    try {
        $resp = Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec $probeTimeoutSec
        $txt = $resp.Content
        $hasClass = $txt.IndexOf('"class"') -ge 0
        $hasList = $txt.IndexOf('"list"') -ge 0
        if ($resp.StatusCode -eq 200 -and ($hasClass -or $hasList)) {
            $ok = $true
            if ($hasClass -and $contentDeny.Count -gt 0) {
                try {
                    $parsed = $txt | ConvertFrom-Json
                    $hay = (@($parsed.class | ForEach-Object { $_.type_name } | Where-Object { $_ }) -join ' ')
                    foreach ($w in $contentDeny) {
                        if ($hay.IndexOf($w, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
                            $ok = $false
                            $adultHit = $true
                            break
                        }
                    }
                } catch {
                    $ok = $true
                }
            }
        }
    } catch {
        $ok = $false
    }
    if ($adultHit) {
        [void]$adult.Add($s)
        Write-Output "ADULT: $name ($url) — content-layer deny hit"
    } elseif ($ok) {
        [void]$alive.Add($s)
        Write-Output "ALIVE: $name ($url)"
    } else {
        [void]$dead.Add($s)
        Write-Output "DEAD : $name ($url)"
    }
}

# 剔除失效源时，记录到防抖状态，供 source-import 冷却用
# 成人源剔除时，记录到永久黑名单（blocked），不会进入冷却重入
if ($dead.Count -gt 0 -or $adult.Count -gt 0) {
    $statePath = if ($env:IMPORT_STATE) { $env:IMPORT_STATE } else { ".source-import-state.json" }
    $state = if (Test-Path $statePath) { (Get-Content $statePath -Raw -Encoding UTF8 | ConvertFrom-Json) } else { @{ removed = [pscustomobject]@{} } }
    if ($null -eq $state.removed) { $state.removed = @{} }
    if ($null -eq $state.blocked) { $state | Add-Member -Name "blocked" -Value ([hashtable]@{}) -MemberType NoteProperty -Force }
    foreach ($s in $dead) {
        # key 规范化与 source-import.ps1 的 Get-CanonicalApi 保持一致（http→https / 去尾斜杠 / 小写）
        $key = $s.api.Trim()
        if ($key -match '^http://') { $key = "https://" + $key.Substring(7) }
        $key = $key.TrimEnd('/').ToLowerInvariant()
        $rec = New-Object PSObject -Property @{ removedAt = (Get-Date -Format "yyyy-MM-dd"); onlineStreak = 0 }
        $state.removed | Add-Member -Name $key -Value $rec -MemberType NoteProperty -Force
        Write-Output "REMOVED-STATE: $($s.name) -> $key"
    }
    foreach ($s in $adult) {
        $key = $s.api.Trim()
        if ($key -match '^http://') { $key = "https://" + $key.Substring(7) }
        $key = $key.TrimEnd('/').ToLowerInvariant()
        $rec = New-Object PSObject -Property @{ blockedAt = (Get-Date -Format "yyyy-MM-dd"); reason = "adult-content-deny" }
        $state.blocked | Add-Member -Name $key -Value $rec -MemberType NoteProperty -Force
        Write-Output "BLOCKED-STATE: $($s.name) -> $key"
    }
    $stateFull = [System.IO.Path]::GetFullPath($statePath)
    [System.IO.File]::WriteAllText($stateFull, ($state | ConvertTo-Json -Depth 6), (New-Object System.Text.UTF8Encoding($false)))
}

if ($alive.Count -gt 0) {
    $json = $alive | ConvertTo-Json -Depth 4
    [System.IO.File]::WriteAllText((Resolve-Path $listPath), $json, (New-Object System.Text.UTF8Encoding $false))
}
Write-Output "SUMMARY: total=$($sources.Count) alive=$($alive.Count) dead=$($dead.Count) adult=$($adult.Count)"
