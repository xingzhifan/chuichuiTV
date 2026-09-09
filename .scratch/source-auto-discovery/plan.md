# 源自动发现 + 合入 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 让 `update-sources.yml` 每日定时任务新增"自动发现并合入上游新采集源"步骤：拉取 `mylazily/ziyuanzhan` 的 `online.json` → 过滤成人源/死源 → 去重 → 3 天冷却防抖 → 按质量排序 → 写回 `sources.json`。

**架构：** 新增 `source-import.ps1`（上游发现），复用既有 `probe-sources.ps1`（存活维护），两者按 `import → probe` 顺序在同一 job 串联。质量排序只作用于 `sources.json` 条目顺序，不改 `{name,api,type}` 结构与任何 app 代码。防抖状态落 `.source-import-state.json`。

**技术栈：** PowerShell 5.1 / pwsh 7 双兼容（CI 为 `windows-latest` 的 pwsh），Gitee/Actions `workflow_dispatch` + cron。本机无 pwsh，用 `powershell -NoProfile -File ...` 验证。

**规格：** `.scratch/source-auto-discovery/spec.md`（已批准）。计划放 `.scratch/source-auto-discovery/plan.md`（仓库惯例；skill 默认路径不适用）。

---

## 文件结构

| 文件 | 职责 |
|------|------|
| `source-deny.txt` | 黑词表，每行一词；`#` 注释。首版词表见任务 2。 |
| `source-import.ps1` | 上游发现 + 过滤器链 + 去重 + 防抖 + 质量排序 + 写回（含 `-DryRun`、`-SkipProbe`、`-UpstreamUrl`）。核心函数 + 主流程。 |
| `.source-import-state.json` | 防抖状态 `{"removed":{"<api>":{"removedAt":...,"onlineStreak":N}}}`，Git 跟踪。初始 `{"removed":{}}`。 |
| `probe-sources.ps1` | （修改）剔除失效源时写入 `.source-import-state.json`，其余逻辑不变。 |
| `.github/workflows/update-sources.yml` | （修改）增加 step A `import`；提交范围扩为 3 个文件；保留既有 checkout 去硬编码注释。 |
| `tests/fixtures/upstream-good.json` | 假上游在线列表（含正常源/成人源/死源/已有源/待冷却源）。 |
| `tests/fixtures/sources-initial.json` | 初始 `sources.json` 快照，供断言脚本隔离测试。 |
| `tests/source-import.tests.ps1` | 断言脚本（纯函数级 + 合入场景级），`powershell -NoProfile -File` 运行，退出码 0=全过。 |

---

### 任务 1：`source-deny.txt` + 初始状态文件（纯资源，无逻辑）

**文件：**
- 创建：`source-deny.txt`
- 创建：`.source-import-state.json`

- [ ] **步骤 1：写黑词表**

`source-deny.txt`（每行一词，`#` 注释）：

```
# 成人向/擦边源黑词表 — 匹配 name/api/description（忽略大小写子串）
老色
色猫
成人
色情
黄av
搜av
白嫖
美少女
x水机
香x儿
豆豆
麻豆
番号
湿乐园
杏吧
伦理
91
```

> 注意：不要单独用 `av`（会误伤 `subocj.com` 等），用 `黄av`/`搜av` 等组合词；`伦理` 同时覆盖分类级成人语义。

- [ ] **步骤 2：写初始状态文件**

`.source-import-state.json`：

```json
{"removed":{}}
```

- [ ] **步骤 3：验证两个文件存在且文本正确**

运行：
```powershell
Get-Content source-deny.txt; Get-Content .source-import-state.json
```
预期：黑词表逐词一行；state 文件恰好输出 `{"removed":{}}`。

- [ ] **步骤 4：Commit**

```bash
git add source-deny.txt .source-import-state.json
git commit -m "chore(sources): add deny-word list and initial import state"
```

---

### 任务 2：`source-import.ps1` 纯函数（解析/去重/排序/过滤）

**文件：**
- 创建：`tests/fixtures/upstream-good.json`
- 创建：`tests/source-import.tests.ps1`（本任务仅测纯函数）
- 创建：`source-import.ps1`（本任务先交付函数区，主流程占位由任务 3 完整化——可整体写，函数即定）

- [ ] **步骤 1：写 fixture** `tests/fixtures/upstream-good.json`：

```json
{
  "data": [
    {"id": 1, "name": "新极品资源站", "api": "https://jipinvip1.com/api.php/provide/vod/", "rating": 4.8, "responseTime": 693, "status": "ok", "health": {"is_alive": true}},
    {"id": 2, "name": "360资源站", "api": "https://360zy.com/api.php/provide/vod", "rating": 4.5, "responseTime": 952, "status": "ok", "health": {"is_alive": true}},
    {"id": 3, "name": "老色X资源站", "api": "https://apilsbzy1.com/api.php/provide/vod/", "rating": 4.2, "responseTime": 393, "status": "ok", "health": {"is_alive": true}},
    {"id": 4, "name": "无限资源站", "api": "https://api.wujinapi.me/api.php/provide/vod/", "rating": 4.5, "responseTime": 354, "status": "ok", "health": {"is_alive": true}},
    {"id": 5, "name": "已死资源站", "api": "https://dead.example.com/api.php/provide/vod/", "rating": 4.3, "responseTime": 100, "status": "down", "health": {"is_alive": false}}
  ]
}
```

- [ ] **步骤 2：写失败的纯函数断言** `tests/source-import.tests.ps1`（本任务的断言入口，后续任务追加）：

```powershell
$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "..\source-import.ps1") -NoMain

$fail = 0
function Assert([bool]$cond, [string]$msg) {
    if (-not $cond) { $fail += 1; Write-Host "FAIL: $msg" } else { Write-Host "ok: $msg" }
}

# Get-CanonicalApi
$c1 = Get-CanonicalApi "https://jipinvip1.com/api.php/provide/vod/"
$c2 = Get-CanonicalApi "https://jipinvip1.com/api.php/provide/vod"
Assert ($c1 -eq $c2) "canonical api trims trailing slash"
Assert (Get-CanonicalApi "HTTP://360ZY.com/api.php/PROVIDE/vod/" -eq "https://360zy.com/api.php/provide/vod") "scheme+domain normalized, path lowercased"

# Test-DenyEntry
Assert (Test-DenyEntry @{name="老色X资源站"; api="https://apilsbzy1.com"; description=""} @("老色","伦理")) "deny hits name"
Assert (-not (Test-DenyEntry @{name="奥迪资源站"; api="https://aodi.com"; description="无广告"} @("老色","伦理"))) "clean name passes"
Assert (Test-DenyEntry @{name="影视站"; api="https://x.com/91/vod"; description=""} @("老色","伦理","91")) "deny hits api substring"

# Test-AcListAlive (offline: uses SkipProbe via env, see task 3) — placeholder assertion removed: HTTP only in CI

# Convert-ToSource
$src = Convert-ToSource @{name="新极品资源站"; api="https://jipinvip1.com/api.php/provide/vod/"; rating=4.8; responseTime=693}
Assert ($src.name -eq "新极品资源站" -and $src.api -eq "https://jipinvip1.com/api.php/provide/vod" -and $src.type -eq "maccms") "to-source maps fields and strips trailing slash"

# Get-SortKeys / ordering handled in task 3 fixture merge test

if ($fail -gt 0) { exit 1 }
Write-Host "ALL PASS"
exit 0
```

- [ ] **步骤 3：运行断言，确认失败（函数不存在）**

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File tests\source-import.tests.ps1
```
预期：脚本报错「无法识别…Get-CanonicalApi」或以某种方式 FAIL（红）。

- [ ] **步骤 4：写 `source-import.ps1` 函数区（含 `-NoMain` 开关，供测试 dot-source）**

```powershell
param(
    [switch]$DryRun,
    [switch]$SkipProbe,
    [string]$UpstreamUrl = "https://mylazily.github.io/ziyuanzhan/data/online.json",
    [switch]$NoMain
)
$ErrorActionPreference = "Stop"
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
```

- [ ] **步骤 5：运行断言，确认通过**

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File tests\source-import.tests.ps1
```
预期：`ok:` 若干行 + `ALL PASS`，退出码 0。

- [ ] **步骤 6：改 `.gitignore` 前的可测试产物已就绪 → 先跑一次带 `-NoMain` 的脚本确认无主流程副作用**

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File source-import.ps1 -NoMain
```
预期：退出 0，不产生任何输出（函数声明不执行主流程）。

- [ ] **步骤 7：Commit**

```bash
git add source-import.ps1 tests/source-import.tests.ps1 tests/fixtures/upstream-good.json
git commit -m "feat(sources): canonical api, deny filter, to-source helpers for import"
```

---

### 任务 3：`source-import.ps1` 主流程（合入 + 排序 + 防抖 + 写回）

**文件：**
- 创建：`tests/fixtures/sources-initial.json`
- 修改：`tests/source-import.tests.ps1`（追加防抖与排序断言）
- 修改：`source-import.ps1`（追加主流程函数 + `-NoMain` 保护）

- [ ] **步骤 1：写初始清单 fixture** `tests/fixtures/sources-initial.json`（含已在库的 360，用于验证按 api 去重不重复合入）：

```json
[
  {"name": "魔都资源", "api": "https://www.mdzyapi.com/api.php/provide/vod/", "type": "maccms"},
  {"name": "360资源",  "api": "https://360zy.com/api.php/provide/vod",       "type": "maccms"},
  {"name": "采集聚合", "api": "http://cj.lziapi.com/api.php/provide/vod/",   "type": "maccms"}
]
```

> 预期合并结果（现有 3 + 新发现 2 新极品/无限 = 5，按 rating 降序→resp 升序）：
> `[0] 新极品(4.8)` `[1] 无限(4.5/354)` `[2] 360(4.5/952)` `[3] 魔都(无上游→0)` `[4] 采集聚合(无上游→0)`

- [ ] **步骤 2：追加失败断言**（到 `tests/source-import.tests.ps1` 尾部，替换掉任务 2 里的占位注释行）：

```powershell
# ---- task 3: Invoke-ImportMerge (pure, offline) ----
$env:SKIP_PROBE = "1"
$up = Get-Content (Join-Path $PSScriptRoot "fixtures\upstream-good.json") -Raw | ConvertFrom-Json
$initial = Get-Content (Join-Path $PSScriptRoot "fixtures\sources-initial.json") -Raw | ConvertFrom-Json
$deny = Get-IpList (Join-Path $PSScriptRoot "..\source-deny.txt") | Where-Object { $_ -and -not $_.StartsWith("#") }
$state = @{ removed = @{} }
$merged = Invoke-ImportMerge $up $initial $deny $state
$apis = @($merged | ForEach-Object { $_.api })
Assert ($apis -contains "https://jipinvip1.com/api.php/provide/vod") "new good source merged"
Assert (-not ($apis -contains "https://apilsbzy1.com/api.php/provide/vod")) "deny-word source not merged"
Assert (-not ($apis -contains "https://dead.example.com/api.php/provide/vod")) "dead source not merged"
Assert (@($apis | Where-Object { $_ -eq "https://360zy.com/api.php/provide/vod" }).Count -eq 1) "existing source kept, not duplicated"
Assert ($merged.Count -eq 5) "expected 5 (3 existing + 2 new)"
$sorted = @($merged | ForEach-Object { $_.api })
Assert ($sorted[0] -eq "https://jipinvip1.com/api.php/provide/vod") "sort rating desc: 4.8 first"
Assert ($sorted[1] -eq "https://api.wujinapi.me/api.php/provide/vod" -and $sorted[2] -eq "https://360zy.com/api.php/provide/vod") "tie on rating 4.5 sorted by resp asc (354 < 952)"
Assert ($sorted[4] -eq "http://cj.lziapi.com/api.php/provide/vod") "existing source without upstream data last"

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
```

- [ ] **步骤 3：运行断言，确认失败**

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File tests\source-import.tests.ps1
```
预期：到「task 3」行报错 `Invoke-ImportMerge` / `Invoke-CooldownStep` 未定义（红）。

- [ ] **步骤 4：写主流程函数**（追加到 `source-import.ps1` 函数区，`-NoMain` 保护段之前）：

```powershell
function Wait-Nothing {}   # no-op, keeps function block balanced

function Write-Sources([array]$list, [string]$path) {
    $arr = @($list | ForEach-Object { @{ name = $_.name; api = $_.api; type = "maccms" } })
    $json = $arr | ConvertTo-Json -Depth 4
    $full = [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $path))
    [System.IO.File]::WriteAllText($full, $json, (New-Object System.Text.UTF8Encoding($false)))
}

function Invoke-CooldownStep([hashtable]$state, [string]$api) {
    $removed = $state.removed
    if ($null -ne $removed -and $removed.PSObject.Properties.Name -contains $api) {
        $rec = $removed.$api
        $newStreak = [int]$rec.onlineStreak + 1
        $reAdd = $newStreak -ge $COOLING_DAYS
        return @{ reAdd = $reAdd; onlineStreak = $newStreak; removedKey = $api }
    }
    return @{ reAdd = $true; onlineStreak = $null; removedKey = $null }
}

function Invoke-ImportMerge($upstream, $existing, [string[]]$deny, [hashtable]$state) {
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
        if (-not $e.health.is_alive) { Write-Output "SKIP-DEAD: $($e.name)"; continue }
        if (Test-DenyEntry $e $deny) { Write-Output "SKIP-DENY: $($e.name)"; continue }
        if (-not ($e.api -match '^https?://')) { Write-Output "SKIP-NOURL: $($e.name)"; continue }
        $canon = Get-CanonicalApi $e.api
        if ($existingMap.ContainsKey($canon) -or $seen.ContainsKey($canon)) { Write-Output "SKIP-DUP: $($e.name)"; continue }
        $cd = Invoke-CooldownStep $state $canon
        if ($cd.reAdd -eq $false) {
            Write-Output "COOLING: $($e.name) (streak $($cd.onlineStreak)/$COOLING_DAYS)"
            # persist streak bump back into state
            $rec = $state.removed.$canon
            $rec.onlineStreak = $cd.onlineStreak
            continue
        }
        if (-not (Test-AcListAlive $e.api $probeTimeoutSec)) { Write-Output "SKIP-UNRESPONSIVE: $($e.name)"; continue }
        $seen[$canon] = $true
        Write-Output "NEW: $($e.name) ($canon)"
        [void]$out.Add(@{
            name = $e.name; api = $e.api; type = "maccms"
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
```

- [ ] **步骤 5：加主流程（在 `param` 里 `-NoMain` 已经声明；末尾加保护段）**：

```powershell
if ($NoMain) { return }

# ---- main ----
$deny = Get-IpList (Join-Path $PSScriptRoot "source-deny.txt") | Where-Object { $_ -and -not $_.StartsWith("#") }
$state = if (Test-Path $statePath) { (Get-Content $statePath -Raw | ConvertFrom-Json) } else { @{ removed = @{} } }
$existing = if (Test-Path $listPath) { @(@(Get-Content $listPath -Raw | ConvertFrom-Json)) } else { @() }
$upstreamRaw = if ($UpstreamUrl -match '^file://') {
    Get-Content ($UpstreamUrl -replace '^file://', '') -Raw | ConvertFrom-Json
} else {
    (Invoke-WebRequest -UseBasicParsing -Uri $UpstreamUrl -TimeoutSec $probeTimeoutSec).Content | ConvertFrom-Json
}
$merged = Invoke-ImportMerge $upstreamRaw $existing $deny $state

if ($DryRun) {
    Write-Output "DRYRUN: would write $(@($merged).Count) sources to $listPath"
    exit 0
}

Write-Sources $merged $listPath
$stateFull = [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $statePath))
[System.IO.File]::WriteAllText($stateFull, ($state | ConvertTo-Json -Depth 6), (New-Object System.Text.UTF8Encoding($false)))
Write-Output "SUMMARY: imported=$(@($merged).Count)"
```

> 说明：`Write-Sources` 用 `ConvertTo-Json` 会带尾随空格/缩进差异，拼回 `{name,api,type}`；已存在的「采集聚合」保留原 http URL（其 api 不被 `Get-CanonicalApi` 改写，只用于去重键）。
> `$env:SKIP_PROBE=1` 仅在测试/离线演练时生效；CI 默认联网探活（任务 5 中 workflow 不用该 env）。

- [ ] **步骤 6：运行断言，确认通过**

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File tests\source-import.tests.ps1
```
预期：全部 `ok:` + `ALL PASS`，退出码 0。含 task 3 断言（4 条合并 + 3 条冷却机）。

- [ ] **步骤 7：Commit**

```bash
git add source-import.ps1 tests/source-import.tests.ps1 tests/fixtures/sources-initial.json
git commit -m "feat(sources): import pipeline with merge, cooldown, ordering, write-back"
```

---

### 任务 4：`probe-sources.ps1` 记录剔除状态（防抖输入）

**文件：**
- 修改：`probe-sources.ps1`

- [ ] **步骤 1：改探活脚本写剔除记录**（在 `foreach` 主循环后、`SUMMARY` 前插入）：

```powershell
# 剔除失效源时，记录到防抖状态，供 source-import 冷却用
if ($dead.Count -gt 0) {
    $statePath = if ($env:IMPORT_STATE) { $env:IMPORT_STATE } else { ".source-import-state.json" }
    $state = if (Test-Path $statePath) { (Get-Content $statePath -Raw | ConvertFrom-Json) } else { @{ removed = @{} } }
    if ($null -eq $state.removed) { $state.removed = @{} }
    foreach ($s in $dead) {
        $key = $s.api.TrimEnd('/').ToLowerInvariant()
        $rec = New-Object PSObject -Property @{ removedAt = (Get-Date -Format "yyyy-MM-dd"); onlineStreak = 0 }
        $state.removed | Add-Member -Name $key -Value $rec -MemberType NoteProperty -Force
        Write-Output "REMOVED-STATE: $($s.name) -> $key"
    }
    [System.IO.File]::WriteAllText((Resolve-Path $statePath), ($state | ConvertTo-Json -Depth 6), (New-Object System.Text.UTF8Encoding($false)))
}
```

- [ ] **步骤 2：本机模拟一次剔除路径**（不动真清单）

```powershell
$env:IMPORT_STATE = ".\tmp-state-test.json"
Remove-Item -Force .\tmp-state-test.json -ErrorAction SilentlyContinue
```
然后临时伪造一条死源探活（只跑 form 验证末尾记录写入逻辑不可行 → 改用手工调用函数方式验证较繁）。**替代验证**：直接 `Set-Content` 一个 `tmp-probe.json` 含两备好探活端点不可靠，故改为**构造最小回归**：用当前真实 `sources.json` 跑 `probe-sources.ps1`，确认其照常 `ALIVE/DEAD`，且当第二轮有死源时 `REMOVED-STATE` 输出出现。

> 说明：CI 场景必然出现（上游持续变更），本地不强行制造死源。本步骤验收=脚本可运行 + 新代码不干扰原逻辑：

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File probe-sources.ps1
```
预期：输出若干 `ALIVE:` 与 `SUMMARY: total=… alive=… dead=…`；无异常。若其探测的网络当前均在线，`dead=0` 正常，不写 state 也正常（`dead.Count -gt 0` 保护）。

- [ ] **步骤 3：Commit**

```bash
git add probe-sources.ps1
git commit -m "feat(sources): record removed sources for import cooldown"
```

---

### 任务 5：workflow 串联（import → probe → 提交）

**文件：**
- 修改：`.github/workflows/update-sources.yml`

- [ ] **步骤 1：在 probe 之前插入 import step，并扩提交范围**（全文件成稿）：

```yaml
name: update-sources

# 每天自动：发现/合入上游新源 + 探活剔除失效源 + 回写；有变化自动提交。
# 推送 sources.json 等文件到仓库后，配合 GitHub Pages（Settings → Pages → Deploy from a branch）即可公网拉取。
on:
  schedule:
    - cron: "30 6 * * *"
  workflow_dispatch: {}
  push:
    paths: ["sources.json", "source-deny.txt", ".source-import-state.json", "probe-sources.ps1", "source-import.ps1", ".github/workflows/update-sources.yml"]

permissions:
  contents: write

jobs:
  sync:
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@v4
        # 用仓库默认分支，不硬编码 ref，避免分支名不匹配导致 checkout 失败

      - name: Import upstream sources
        shell: pwsh
        run: ./source-import.ps1

      - name: Probe sources and rewrite sources.json
        shell: pwsh
        run: ./probe-sources.ps1

      - name: Commit if changed
        shell: bash
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"
          if git diff --quiet HEAD -- sources.json .source-import-state.json source-deny.txt; then
            echo "no change"
          else
            git add sources.json .source-import-state.json source-deny.txt
            git commit -m "chore(sources): auto sync sources (import + prune)"
            git push
          fi
```

- [ ] **步骤 2：仅验证 YAML 可被 PowerShell/CI 解析 ≠ 本地无解析器** → 用 Git 检查 diff 符合预期：

```bash
git diff .github/workflows/update-sources.yml
```
预期：job 名称改 `sync`、多出 `Import upstream sources` step、提交范围含两个新文件、无 `ref: master` 残留。

- [ ] **步骤 3：Commit**

```bash
git add .github/workflows/update-sources.yml
git commit -m "ci(sources): import upstream before probe, widen commit set"
```

---

### 任务 6：端到端本地演练 + 全量验证

**文件：**
- 修改：`tests/source-import.tests.ps1`（追加一个端到端 dry-run 断言，可选如无则跳过）

- [ ] **步骤 1：隔离临时目录跑全链路（import dry-run → probe 逻辑不动真清单）**

```bash
powershell -NoProfile -ExecutionPolicy Bypass -Command "$env:SOURCE_LIST='tmp-import-sources.json'; $env:IMPORT_STATE='tmp-import-state.json'; & '.\source-import.ps1' -UpstreamUrl file://$((Resolve-Path '.\tests\fixtures\upstream-good.json').Path) -SkipProbe -DryRun; Remove-Item .\tmp-import-sources.json -ErrorAction SilentlyContinue; Remove-Item .\tmp-import-state.json -ErrorAction SilentlyContinue"
```
预期：输出 `NEW: 新极品资源站 (...)`、`NEW: 无限资源站 (...)`、`SKIP-DENY: 老色X资源站`、`SKIP-DEAD: 已死资源站`、`SKIP-DUP: 360资源站`、`DRYRUN: would write 5 sources`，**不动仓库内真实文件**。

- [ ] **步骤 2：真实清单不加新源时，跑 dry-run 确认幂等**

```bash
powershell -NoProfile -ExecutionPolicy Bypass -Command "& '.\source-import.ps1' -UpstreamUrl https://mylazily.github.io/ziyuanzhan/data/online.json -DryRun -SkipProbe"
```
预期：输出大量 `SKIP-DUP:`（现有 5 源均在上游），无 `sources.json` 变更（主动检查 `git status --short sources.json`）。

> 说明：`-SkipProbe` 用于免网络；真实 CI 不设该开关，会逐个 `?ac=list` 探活新候选。

- [ ] **步骤 3：跑完整单测**

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File tests\source-import.tests.ps1
```
预期：`ALL PASS`，退出码 0。

- [ ] **步骤 4：CI 自检（无 Push 的 workflow 语法）**——本地无法模拟 Actions；做静态核对：browse workflow diff 无拼写错误（`./source-import.ps1` 与脚本文件名一致）。

- [ ] **步骤 5：Commit（若有新测试断言）**

```bash
git add tests/source-import.tests.ps1
git commit -m "test(sources): e2e dry-run and idempotency verification"
```
若上一步无新增文件则跳过本 commit。

---

## 自检

**1. 规格覆盖度：**

| 规格章节 | 对应计划任务 |
|---|---|
| 1 数据流（import→probe→提交） | 任务 5 |
| 2 过滤器链（is_alive / 黑词 / api 形状 / ac=list 探活） | 任务 2（deny）+ 3（is_alive, MERGE 内探活） |
| 3 去重（规范化 api） | 任务 2（Get-CanonicalApi）+ 3（Invoke-ImportMerge SKIP-DUP） |
| 4 防抖冷却（3 天） | 任务 3（Invoke-CooldownStep）+ 4（probe 写状态） |
| 5 排序写回 | 任务 3（Sort-Object rating desc → resp asc；无上游数据排尾） |
| 6 文件与提交（deny.txt/state/import.ps1/workflow/probe） | 任务 1/2/3/4/5 各自成稿 |
| 7 测试（dry-run / fixture / 双跑） | 任务 3/6 |

覆盖无遗漏；`publish-sources/` 非目标（规格 Non-goals），未列入。

**2. 占位符扫描：**
- 未写「待定 / 后续实现 / 适当错误处理」等。
- 任务 4 步骤 2 曾考虑占位验证，已改写为「真清单可运行 + dead.Count 保护」的具体命令。
- 每个含代码的任务都给出可运行命令与预期输出。

**3. 类型/命名一致性：**
- 函数名统一：`Get-CanonicalApi` / `Test-DenyEntry` / `Convert-ToSource` / `Get-IpList` / `Test-AcListAlive` / `Write-Sources` / `Invoke-CooldownStep` / `Invoke-ImportMerge` / `Find-UpstreamMeta`，测试引用一致。
- `COOLING_DAYS=3`、`state.removed.<api>.onlineStreak`、`removedAt` 键名在脚本/测试/夹具/工作流日志中一致。
- `-NoMain` 开关在 param 声明、测试 dot-source、脚本尾部保护段三处一致。
- 排序字段 `rating`（double 降序）→ `resp`（int 升序）在 merge 与断言中一致。
- `source-deny.txt` 黑词表词条与测试断言（老色/伦理/91）一一对应。