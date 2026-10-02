# publish-tvbox.ps1 - 把本仓库的 sources.json 转成开源端能读的配置
#
#   sites.json  {"sites":[...]}  单仓：原版 TVBox / FongMi 影视 / OK影视 / 影视仓 填这个
#   tvbox.json  {"urls":[...]}   多仓入口：影视仓、宝盒 这类多仓壳子填这个
#
# 由 .github/workflows/update-sources.yml 在探活之后调用（每天自动跟着更新）；
# 也可以本地手动跑：  pwsh -File publish-tvbox.ps1
#
# 换了仓库名 / 账号，改下面的 $PagesBase（或设环境变量 TVBOX_PAGES_BASE）。
param(
    [string]$ListPath = "sources.json",
    [string]$SitesPath = "sites.json",
    [string]$WarehousePath = "tvbox.json",
    [string]$PagesBase = "",
    [string]$WarehouseName = "锤锤影视 · 采集源"
)

$ErrorActionPreference = "Stop"

$root = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }
if (-not $PagesBase) {
    $PagesBase = if ($env:TVBOX_PAGES_BASE) { $env:TVBOX_PAGES_BASE } else { "https://xingzhifan.github.io/chuichuiTV" }
}
$PagesBase = $PagesBase.TrimEnd('/')

$listFull = if ([System.IO.Path]::IsPathRooted($ListPath)) { $ListPath } else { Join-Path $root $ListPath }
if (-not (Test-Path $listFull)) { throw "找不到 $listFull" }

$raw = [System.IO.File]::ReadAllText($listFull, [System.Text.Encoding]::UTF8)
$parsed = $raw | ConvertFrom-Json

# 兼容三种包裹写法，或直接就是数组
$items = @()
if ($parsed -is [System.Array]) { $items = $parsed }
elseif ($null -ne $parsed.sources) { $items = @($parsed.sources) }
elseif ($null -ne $parsed.list) { $items = @($parsed.list) }
elseif ($null -ne $parsed.data) { $items = @($parsed.data) }

$sites = New-Object System.Collections.Generic.List[object]
$seen = New-Object System.Collections.Generic.HashSet[string]
foreach ($it in $items) {
    if ($null -eq $it) { continue }
    $api = ""
    if ($it.PSObject.Properties.Name -contains "api" -and $it.api) { $api = [string]$it.api }
    elseif ($it.PSObject.Properties.Name -contains "url" -and $it.url) { $api = [string]$it.url }
    $api = $api.Trim()
    if (-not $api) { continue }
    if ($api -notmatch '^https?://') { continue }        # 只认 http(s)，其余开源端会报错
    if (-not $seen.Add($api)) { continue }               # 同一个接口只留一条，重复会让搜索慢一倍

    $name = ""
    if ($it.PSObject.Properties.Name -contains "name" -and $it.name) { $name = ([string]$it.name).Trim() }
    if (-not $name) {
        try { $name = ([uri]$api).Host } catch { $name = "线路" + ($sites.Count + 1) }
    }

    $sites.Add([pscustomobject][ordered]@{
        key         = "src{0:d2}" -f ($sites.Count + 1)
        name        = $name
        type        = 1                                  # 1 = 苹果CMS JSON 接口
        api         = $api
        searchable  = 1
        quickSearch = 1
        filterable  = 1
    })
}

$utf8 = New-Object System.Text.UTF8Encoding($false)      # 无 BOM，Pages / 解析器都认
$sitesJson = [pscustomobject][ordered]@{ sites = $sites.ToArray() } | ConvertTo-Json -Depth 6
[System.IO.File]::WriteAllText((Join-Path $root $SitesPath), $sitesJson, $utf8)

$url = "$PagesBase/$SitesPath"
$whJson = [pscustomobject][ordered]@{ urls = @([pscustomobject][ordered]@{ name = $WarehouseName; url = $url }) } | ConvertTo-Json -Depth 6
[System.IO.File]::WriteAllText((Join-Path $root $WarehousePath), $whJson, $utf8)

Write-Host ("sites.json   : {0} 条线路 -> {1}" -f $sites.Count, (Join-Path $root $SitesPath))
Write-Host ("tvbox.json   : 多仓入口 -> {0}" -f (Join-Path $root $WarehousePath))
Write-Host ("开源端配置地址：{0}" -f $url)
