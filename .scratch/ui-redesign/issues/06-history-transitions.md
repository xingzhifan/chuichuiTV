# 06 — 历史页重做 + 统一页面过渡动画

**What to build:** `HistoryScreen` 改用海报卡片列表（复用 `PosterCard`，保留时间/线路副信息）；`MainActivity` 的 `NavHost` 增加统一、克制的页面过渡动画（系统 `enterTransition/exitTransition` 淡入淡出）。

**Blocked by:** 05

**Status:** resolved（build+单测已验证）

- [x] `HistoryScreen`：纯文本 → 海报卡片行（小海报 + 片名 + 线路/时间副信息），保留点击重建 `PlayRequest` 的能力。
- [x] `MainActivity` `NavHost`：为各 destination 设置系统 enter/exit transition（克制淡入淡出，遵守性能红线，不用复杂转场）。
- [x] 验证编译 + 单测通过。

## Progress

- `HistoryScreen`：海报缩略（2:3 圆角占位，渐变竖条 playing 图案）+ 片名 + 「集 · 线路 · 时间」副信息；点击重建 `PlayRequest`。**说明**：`History` 当前只存 `vodId/vodName/episode/line/ts`、无海报地址，按规定"只换 UI 不动领域数据"，用主题色海报占位而非引入真实图（如后续 `History` 增加 `vodPic` 字段可无缝替换为 `PosterCard`）。
- `MainActivity`：`NavHost` 增加 `enterTransition/exitTransition/popEnterTransition/popExitTransition` 统一 220/180ms 淡入淡出（克制、无滑动/缩放）。
- 验证：`assembleMobileDebug`+`assembleLeanbackDebug`+`testMobileDebugUnitTest` BUILD SUCCESSFUL。
