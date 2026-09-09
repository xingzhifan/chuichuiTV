# 04 — 片源列表 + 搜索 重做

**What to build:** `VodListScreen`（分类列表）与 `SearchScreen`（搜索结果）用 `PosterCard` 海报网格替换纯文本 `ListRow`，保留现有无限滚动/分页（`PagedVodListState` + `AutoLoadMore`）。

**Blocked by:** 03

**Status:** resolved（build+单测已验证）

- [x] `VodListScreen`：`LazyColumn` → `LazyVerticalGrid`（列数随屏幕自适应，手机 2-3 列 / 电视更多），每项 `PosterCard`。
- [x] `SearchScreen`：结果改网格，搜索框 + 搜索按钮视觉与主题统一、保持可用。
- [x] 两者均保留滚动到底自动翻页；加载/空态不破坏网格布局。
- [x] 点击卡片 → 详情（复用现有 `onOpenVod` 回调）。
- [x] 验证编译 + 单测通过（含 `PagedVodListState` 行为不回归）。

## Progress

- `Scaffold.kt` 新增 `AutoLoadMoreGrid(LazyGridState)`（判据同 `AutoLoadMore`，触底阈值 4），供网格滚动翻页。
- `VodListScreen`：改用 `LazyVerticalGrid(GridCells.Adaptive(110.dp))` + `PosterCard`（2:3 海报+标题），保留 `PagedVodListState` 无限滚动；保留 `vodLabel`。
- `SearchScreen`：改用同一海报网格 + `AutoLoadMoreGrid`；保留 old 请求取消(cancel)、分页加载；空态细分（未配置源/无结果/未搜索）。
- 验证：`assembleMobileDebug`+`assembleLeanbackDebug`+`testMobileDebugUnitTest` BUILD SUCCESSFUL。
