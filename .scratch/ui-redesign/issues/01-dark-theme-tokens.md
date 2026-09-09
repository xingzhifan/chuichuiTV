# 01 — 暗色主题 + 设计 Token 系统

**What to build:** 新增 Compose 自定义暗色主题层 `ui/theme/`，覆盖默认 MaterialTheme，作为全站视觉基础（配色、字体、间距）。主色：深蓝黑背景 + 蓝紫渐变高亮（`#5E35B1`→`#2979FF`）+ 白色文字。

**Blocked by:** 无

**Status:** resolved（build+单测已验证）

- [x] 新建 `ui/theme/Color.kt`：定义完整暗色配色集（背景、表面、主色、次色、文字、渐变高亮、卡片、角标等）。
- [x] 新建 `ui/theme/Type.kt`：标题/正文/小字/角标字体的字号与字重。
- [x] 新建 `ui/theme/Theme.kt`：`ChuiChuiTheme { darkColorScheme(...) }`，暴露 `MaterialTheme`。
- [x] `MainActivity.kt`：`MaterialTheme { }` → `ChuiChuiTheme { }`。
- [x] 验证：`assembleMobileDebug` + `assembleLeanbackDebug` 编译通过；`testMobileDebugUnitTest` 通过。

## Progress

- 新增 `ui/theme/`：`Color.kt`（深蓝黑底 `#0F1115` + 蓝紫渐变 `#5E35B1`→`#2979FF` + 白色文字 + 语义色 + 渐变遮罩）、`Type.kt`（统一字号层级）、`Theme.kt`（`ChuiChuiTheme` 包裹 `darkColorScheme`，全站统一入口）。
- `MainActivity` 移除默认 `MaterialTheme` 导入，改为 `ChuiChuiTheme { }`。
- 验证：`assembleMobileDebug`+`assembleLeanbackDebug`+`testMobileDebugUnitTest` BUILD SUCCESSFUL（JDK21，JAVA_HOME 需指向 `C:\Program Files\Java\jdk-21`）。

## Review remediation

- **C1：渐变方向** 统一修正为 紫→蓝（`listOf(AccentPurple, AccentBlue)`）。涉及 4 处：`Categories.SourceChip` / `Categories.CategoryTile` / `DetailScreen.LineHeader` / `HistoryScreen.HistoryRow`，其中 3 处顺带收敛为共享组件 `AccentBar`（`Scaffold.kt`），杜绝方向再漂移。
- 顶栏 4 屏重复收敛为 `ScreenTopBar`（`Categories` / `VodListScreen` / `SearchScreen` / `HistoryScreen`）。
- `Scaffold.kt` 删除死代码：`ScreenScaffold` / `ListRow` / `AutoLoadMore`。
- `PosterCard` 底渐用 `BadgeBg` token 取代硬编码 `Color(0x99000000)`。
