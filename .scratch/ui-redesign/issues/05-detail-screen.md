# 05 — 详情页重做

**What to build:** 重做 `DetailScreen`（腾讯式详情）：大封面 + 标题 + 备注/年份信息 + 线路分组 + 集数选择。当前把所有线路的集拉平为"线路 | 集"纯文本行，改造为分线路分组、可点选集的卡片式界面。

**Blocked by:** 04

**Status:** resolved（build+单测已验证）

- [x] 顶部大封面（Coil 加载 `vodPic`）+ 底部渐变遮罩 + 片名/备注/年份（用 `Detail.vod` 字段）。
- [x] 线路分组展示：按 `Detail.lines`（每条 `Line` 有其 `name` + `episodes`）分组渲染。
- [x] 集数选择：可点击选集（沿用现有 `onPlay(PlayRequest)` 回调，携带线路信息）。
- [x] 视觉统一到主题；保留加载/空态。
- [x] 验证编译 + 单测通过（`Detail`/`Line`/`Episode` 解析逻辑不动，只换 UI）。

## Progress

- `DetailScreen`：加载完整 `Detail`（含 `lines`），`LazyVerticalGrid(GridCells.Adaptive(96.dp))` 一个网格承载全部：
  - 头图 `DetailHeader`：全宽 16:9 `vodPic`(Coil) + 底部渐变遮罩(`ScrimBottom`) + 片名/类型/年份/备注叠加。
  - `LineHeader`：蓝紫渐变竖条 + 线路名。
  - `EpisodeChip`：圆角集数卡片，点击 `onPlay(PlayRequest(vodId, vodName, ep.name, line.name))`。
  - `span={ GridItemSpan(maxLineSpan) }` 让头图/线路标题占满整行。
- 验证：`assembleMobileDebug`+`assembleLeanbackDebug`+`testMobileDebugUnitTest` BUILD SUCCESSFUL。

## Review remediation

- **C2 决议：头图保持 16:9**（腾讯式横幅头图，列表 2:3、详情 16:9 并存是刻意的，不违反 ADR-0005）。
- 渐变方向统一紫→蓝（`Issue 01 C1` 连带修复，头图 `ScrimBottom` 遮罩不受影响）。
