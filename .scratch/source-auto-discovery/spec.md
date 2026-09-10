# 锤锤影视 — 源自动发现 + 合入 规格

> 状态：resolved（2026-09-10 实现完成：6 任务全部落地并审查通过，端到端 dry-run / 幂等 / 单测全部通过，sources.json 未受演练影响）
> 关联：`probe-sources.ps1`、`.github/workflows/update-sources.yml`、`sources.json`、`SOURCES.md`
> 范围：GitHub 管道侧（根仓库），不改 `app/` / `chuichui/` / `modsearch/`；`publish-sources/` 保持独立副本不同步

## 背景 / 动机

当前自动化只做「探活剔除」：`probe-sources.ps1` 每天对各源 `GET {api}?ac=list`，存活保留、失效剔除并回写 `sources.json`。**没有任何自动添加逻辑**，新源只能手动编辑 `sources.json`（或靠 app「批量导入」粘贴），且来源靠人工发现。用户希望：GitHub 上的采集源数量能**自动变多**。

经评估选定上游：**mylazily/ziyuanzhan**（资源站落实测面板，`monitor.py` 每 6 小时从 ziyuanzu API 抓取并做健康检查，静态 JSON 直接可摄取）：

- `https://mylazily.github.io/ziyuanzhan/data/online.json` — 全部「在线」资源站
- `https://mylazily.github.io/ziyuanzhan/data/offline.json` — 离线资源站
- `https://mylazily.github.io/ziyuanzhan/data/latest.json` — 全量数据

`online.json` 条目结构（2026-09-09 实测共 62 个）：`name`、`link`、`source_url`、`api`（采集接口地址，如 `https://x.com/api.php/provide/vod/`）、`description`、`status`、`uptime`、`resource_count`、`rating`、`tags`、`speed`、`responseTime`、`todayUpdates`、`health.{url,status_code,response_time_ms,is_alive}`。`api` 字段可直接映射 `sources.json` 的 `{name, api, type:"maccms"}`。

上游存在大量成人向/擦边源（老色X、色猫、搜av、白嫖、黄av、美少女、X水机、香X儿、豆豆、麻豆、黄色仓库、番号、湿乐园、杏吧…），仓库现状 `SOURCES.md` 明确「仅包含常规影视源；成人向源已剔除」，故自动合入必须带过滤。

## 功能规格

### 1. 数据流

```
GitHub Actions (每日 06:30 UTC)
    │
    ├─ step A  source-import.ps1（自动发现 + 合入 + 排序）
    │     拉取 online.json → 过滤器链 → 去重 → 防抖 → 追加合入 → 全量按质量排序 → 写回 sources.json
    │
    └─ step B  probe-sources.ps1（现有探活；剔除失效源时记录防抖状态）
          对 sources.json 全量 ac=list 探活 → 存活保留 / 失效剔除 + 写入 .source-import-state.json
```

workflow 有实质变化则自动提交（`sources.json`、`.source-import-state.json`、`source-deny.txt`）。

### 2. 过滤器链（step A，任一不过即丢弃）

对上游 `online.json` 每个条目依次套用：

1. **上游在线**：`health.is_alive == $true`（上游判定在线；`status` 仅参考，以其本地健康检查为准）。
2. **非成人（元数据层）**：`name`/`api`/`description`（忽略大小写，子串匹配）不命中 `source-deny.txt` 黑词表。首版黑词表覆盖：`老色、色猫、成人、色情、黄、av资源、搜av、白嫖、美少女、x水机、香x儿、豆豆、麻豆、番号、湿乐园、杏吧、伦理、91`。
3. **非成人（内容层，2026-09-10 新增）**：对新候选 `GET {api}?ac=list` 返回的 JSON 解析 `class[].type_name`（分类名），任一命中 `source-content-deny.txt` 强词表 → `SKIP-ADULT` 拒绝，并写入 `state.blocked` **永久封禁**（不入冷却，避免重入）。强词表：`成人、色情、情色、无码、有码、偷拍、AV、SM、制服、自拍、丝袜、熟女、激情、口爆`。**弱词「伦理」不触发内容层**（常规站亦有「伦理」分类，误杀太大）；元数据层例外：`伦理` 仍保留在 `source-deny.txt`，因其名字含「伦理」的源基本可判定，实盘验证：0 误杀。
4. **api 形状**：以 `https?://` 开头、以 `vod`/`api.php`/`json.php` 结尾等可被 maccms 探活接受的形态（否则视为不可用接口丢弃）。
5. **maccms 探活**：`GET {api}?ac=list`，`StatusCode -eq 200` 且响应体含 `"class"` 或 `"list"`（复用 `probe-sources.ps1:39-41` 的同一判据）。失败 → 丢弃并打印 `SKIP-UNRESPONSIVE`。**逐条带超时（默认 12s）**；为控时长，新候选一般 ≤ 上游在线数（≈ 60 上限）。

> **存量清理**：`probe-sources.ps1`（step B）对 `sources.json` 全量探活时同步做内容层检查——返回分类命中强词表 → `ADULT` 剔除并写入 `state.blocked`（永久，不参与冷却重入）。一次 Action 运行即「import 拦新增 + probe 清存量」双保险。

### 3. 去重

- 去重键：`api`（规范化：去尾斜杠、去除空白、统一 `http`/`https`）。
- 与**现有 `sources.json`** 的 api 相同 → 跳过（现有 5 源中 魔都/360/爱奇艺/天涯 都在上游，自动避开，不产生重复）。
- 上游列表内部按 api 去重（同源重复条目取第一条）。

### 4. 防抖：剔除后重入需冷却期

现有探活**永久剔除**失效源；上游每 6h 重检、常把「本地恢复」的站重新标为在线（实测数据里魔都/飘零/色猫等有多处「本地检测恢复，ziyuanzu 仍显示 down」）。若不加控制会「今天剔除 → 明天发现 → 后天再剔除」反复横跳。

- 状态文件 `.source-import-state.json`：
  ```json
  { "removed": { "<api>": { "removedAt": "2026-09-09", "onlineStreak": 0 } },
    "blocked":  { "<api>": { "blockedAt": "2026-09-10", "reason": "adult-content-deny" } } }
  ```
- **step B（probe）**剔除任一「失效」源时，写入/更新该 api 到 `removed`：`removedAt=today`、`onlineStreak=0`。
- **step B（probe）**剔除任一「成人内容」源时，写入该 api 到 `blocked`（`reason=adult-content-deny`），**永久封禁**。
- **step A（import）**发现候选源时：
  - 命中 `blocked` 表 → 直接 `SKIP-BLOCKED`（永久，不参与冷却）。
  - 不在 `removed` 表 → 正常合入（再经内容层过滤，见 §2）。
  - 在 `removed` 表 → `onlineStreak += 1`；仅当 `onlineStreak >= 3`（即连续 3 次定时任务在线）才重新合入，并从 `removed` 表删除；否则跳过（打印 `COOLING`）。
- 手动编辑进 `sources.json` 的源不受防抖影响（只在自动剔除/自动重入路径生效）。

### 5. 排序与写回

每次 step A 合入完成后，对**整个 `sources.json`** 按质量排序后写回：

- 主序：上游 `rating` 降序（数值）。
- 次序：`responseTime` 升序（毫秒，数值）。
- 已有但上游无数据的源（如「采集聚合」lziapi 不在上游列表）：`rating=0`、`responseTime=Int32.MaxValue` 排到末尾。
- 排序只影响**顺序**，由 app「批量导入」按列表顺序导入即可，**不改 app 代码**。

### 6. 文件与提交

- 新增：`source-import.ps1`（发现/过滤/去重/防抖/排序/写回，`$ErrorActionPreference="Stop"`，无输入源时 `exit 1`）。
- 新增：`source-deny.txt`（纯文本黑词表，每行一词，`#` 注释行忽略）。
- 新增：`.source-import-state.json`（防抖状态，Git 跟踪，workflow 提交范围内）。
- 修改：`probe-sources.ps1` — 剔除失效源时同时写 `.source-import-state.json`。
- 修改：`.github/workflows/update-sources.yml` — 增加 step A；提交范围扩为 `sources.json .source-import-state.json source-deny.txt`（顺带并入上一轮未提交的 `ref: master` 移除改动）；失败不 `exit 1` 时也打印 `SUMMARY`。
- `sources.json` 结构不变：`[{name, api, type:"maccms"}]`。

### 7. 测试

- 可本地 `pwsh` 演练：`./source-import.ps1 -DryRun`（只打印将要新增/重排的结果，不写文件）。
- 防抖/去重以一次手动构造的临时上游 fixture（离线 JSON + `-UpstreamUrl file://`）验证。
- 完整验证：本地连续跑 `import → probe` 两次，确认状态机（首次合入 / 冷却 / 重入）符合预期；合并结果用 `ConvertFrom-Json` 校验合法。

## 明确不做（Non-goals）

- 不改 app（`app/`）——排序、「批量导入」语义、多源播源顺序均不动。
- `publish-sources/` 独立副本不同步（其自身文件各自独立维护，将来如需要可单独加相同逻辑）。
- 不做人工审批闸门（自动合入 + 黑词过滤 + Actions 日志留痕；用户可随时在 Actions 里查阅每一步的 `NEW/ALIVE/DEAD/SKIP/COOLING` 输出）。
- 不做成人源以外的内容识别（不上游逐条全量采片），只同步源列表。

## 字段 / 接口备忘

- 新增：`source-import.ps1`（`-DryRun`、`-UpstreamUrl`、复用 `HTTP_TIMEOUT_SEC`/`SOURCE_LIST` 环境变量）
- 新增：`source-deny.txt`
- 新增：`source-content-deny.txt`（内容层强词表，见 §2.3）
- 新增：`.source-import-state.json`（`removed.<api>.removedAt` / `onlineStreak` + `blocked.<api>.blockedAt` / `reason`）
- 修改：`probe-sources.ps1`（剔除时记录防抖状态；新增内容层检查 + `blocked` 永久封禁）
- 修改：`.github/workflows/update-sources.yml`（step A + 提交范围含 `source-content-deny.txt`）