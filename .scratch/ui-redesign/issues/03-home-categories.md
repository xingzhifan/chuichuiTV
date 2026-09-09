# 03 — 首页(分类页)重做

**What to build:** 重做 `Categories`：顶部保留源切换 chip 栏（多源并存时的立身之本），其下分类以腾讯式"分组网格/宫格"呈现，告别当前纯文本 `ListRow`。保留右上「搜索/历史/源」入口。

**Blocked by:** 02

**Status:** resolved（build+单测已验证）

- [x] 源选择器重做：更美观的 chip / 分组条样式，仍按稳定 id 切换、跨源切回重读。
- [x] 分类从 `ListRow` 文本改为卡片式网格（可考虑图标/首字块 + 分类名），虚拟化。
- [x] 顶栏「搜索/历史/源」入口保持可用、视觉统一。
- [x] 空态/加载态沿用 `ScreenScaffold` 的骨架但用新视觉。
- [x] 验证编译 + 单测通过。

## Progress

- `Categories` 重做：顶栏（标题+搜索/历史/源 圆角动作文字）→ 多源渐变 chip 选择器（`SourceChip`，选中蓝紫渐变）→ 分类用 `LazyVerticalGrid(GridCells.Adaptive(128.dp))` 渲染 `CategoryTile`（渐变竖条+居中名），告别纯文本 `ListRow`；保留多源切换 + 跨源重读。
- **共享组件迁移**：原定义在 `Categories.kt` 的 `ScreenScaffold`/`SelectChip`/`ListRow`/`LoadMoreRow`/`AutoLoadMore`（被 Detail/History/Search/VodList/Sources 复用）移至新 `ui/Scaffold.kt`，避免重做首页时误删。
- 验证：`assembleMobileDebug`+`assembleLeanbackDebug`+`testMobileDebugUnitTest` BUILD SUCCESSFUL。
